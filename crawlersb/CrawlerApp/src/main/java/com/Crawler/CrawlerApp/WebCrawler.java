package com.Crawler.CrawlerApp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class WebCrawler {

    private static final Logger logger = LogManager.getLogger(WebCrawler.class);

    private final Environment environment;
    private java.sql.Connection dbConnection;
    private int numDocuments;

    @Autowired
    public WebCrawler(Environment environment) {
        this.environment = environment;
        numDocuments = 0;
    }

    private Connection getDatabaseConnection() throws IOException, SQLException {
        String encryptedFileLocation = environment.getProperty("database.encrypted-file-location");
        String encryptionKey = environment.getProperty("database.encryption-key");

        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(encryptedFileLocation)) {
            properties.load(fileInputStream);

            String dbUrl = EncryptionUtil.decrypt(properties.getProperty("database.url"), encryptionKey);
            String dbUsername = EncryptionUtil.decrypt(properties.getProperty("database.username"), encryptionKey);
            String dbPassword = EncryptionUtil.decrypt(properties.getProperty("database.password"), encryptionKey);

            return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        }
    }

    public void connectionToDatabase() {
        try {
            dbConnection = getDatabaseConnection();
            logger.info("Connected to the database.");
        } catch (IOException | SQLException e) {
            logger.error("Failed to connect to the database: {}", e.getMessage());
        }
    }

    public String fetchHtmlContent(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.html();
        } catch (IOException e) {
            logger.error("Error fetching HTML content from URL: {}", url);
            return null;
        }
    }

    private ConcurrentMap<String, Element> urlHtmlContents = new ConcurrentHashMap<>();

    private void fetchAndStoreHtmlContents(List<String> urls) {
        urls.forEach(url -> {
            String htmlContent = fetchHtmlContent(url);
            if (htmlContent != null) {
                Document doc = Jsoup.parse(htmlContent);
                Element body = doc.body();
                urlHtmlContents.put(url, body);
            }
        });
    }

    public List<String> searchDocuments(String searchTerm) {
        logger.info("Received search query: {}", searchTerm);

        // Extract the actual search term without the prefix "query="
        String[] queryParts = searchTerm.toLowerCase().split("=");
        if (queryParts.length != 2) {
            logger.error("Invalid search query format.");
            return Collections.emptyList();
        }

        String actualTerm = queryParts[1].trim();
        logger.info("Actual search term: {}", actualTerm);

        // Split the search term into individual terms
        String[] queryTerms = actualTerm.split("\\s+|\\+");
        logger.info("Query terms: {}", Arrays.toString(queryTerms));

        ConcurrentMap<String, Double> urlScores = new ConcurrentHashMap<>();

        // Calculate the total number of query terms
        int totalQueryTerms = queryTerms.length;

        for (String term : queryTerms) {
            ConcurrentMap<String, Integer> docFrequency = getDocumentFrequencyByTermFromDatabase(term);
            if (docFrequency != null) {
                int df = docFrequency.size();
                double idf = Math.log((double) numDocuments / (df + 1)) + 1;

                // Calculate TF-IDF for each term in the query separately
                int tf_query = 1; // Assuming each term in the query appears once (tf_query = 1)
                double tfidf_query = tf_query * idf;

                for (String url : docFrequency.keySet()) {
                    int tf_doc = docFrequency.get(url);

                    // Only consider documents containing all the query terms
                    if (tf_doc >= totalQueryTerms) {
                        double tfidf_doc = tf_doc * idf;
                        urlScores.put(url, urlScores.getOrDefault(url, 0.0) + tfidf_query * tfidf_doc);
                    }
                }
            }
        }

        List<String> relevantUrls = new ArrayList<>();
        urlScores.entrySet().stream()
                .sorted(ConcurrentMap.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> relevantUrls.add(entry.getKey()));
        logger.info("Relevant URLs found: {}", relevantUrls);

        // Fetch and store the HTML contents of relevant URLs
        fetchAndStoreHtmlContents(relevantUrls);

        return relevantUrls;
    }

    private ConcurrentMap<String, Integer> getDocumentFrequencyByTermFromDatabase(String actualTerm) {
        String sql = "SELECT crawledurls.url, invertedindex.frequency FROM invertedindex " +
                "JOIN crawledurls ON invertedindex.url_hash = crawledurls.url_hash " +
                "WHERE invertedindex.term = ?";

        try (Connection connection = getDatabaseConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, actualTerm);
            // Log the SQL query being executed
            logger.info("Executing SQL query: {}", preparedStatement.toString());
            // Log the SQL query being executed with the actual term value
            logger.info("Executing SQL query with term '{}': {}", actualTerm, preparedStatement.toString());

            ResultSet resultSet = preparedStatement.executeQuery();

            ConcurrentMap<String, Integer> docFrequency = new ConcurrentHashMap<>();
            while (resultSet.next()) {
                String url = resultSet.getString("url");
                int frequency = resultSet.getInt("frequency");
                docFrequency.put(url, frequency);
            }
            if (docFrequency.isEmpty()) {
                logger.info("No document frequency found for term: {}", actualTerm);
            } else {

                logger.info("Term: {}, Document Frequency: {}", actualTerm, docFrequency); // Logging document frequency
            }
            return docFrequency;

        } catch (IOException | SQLException e) {
            logger.error("Error connecting to the database or executing database query: {}", e.getMessage());
            return null;
        }
    }

    public void closeDatabaseConnection() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.error("Error closing the database connection: {}", e.getMessage());
        }
    }

    public ConcurrentMap<String, Element> getUrlHtmlContents() {
        return urlHtmlContents;
    }
}
