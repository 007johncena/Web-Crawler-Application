package Crawlers;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

public class WebCrawler {
    private static final Logger logger = LogManager.getLogger(WebCrawler.class);
    private Queue<String> queue;
    private Set<String> visitedUrls;
    private java.sql.Connection dbConnection;
    private ExecutorService executorService;
    private Map<String, ConcurrentMap<String, Integer>> invertedIndex;
    private int numDocuments;

    public WebCrawler() {
        queue = new LinkedList<>();
        visitedUrls = new HashSet<>();
        executorService = null; // Initialize executorService
        invertedIndex = new ConcurrentHashMap<>();
        numDocuments = 0;
    }

    public void connectionToDatabase(String encryptedPropertiesFile, String encryptionKey) {
        Properties properties = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(encryptedPropertiesFile)) {
            properties.load(fileInputStream);

            String dbUrl = EncryptionUtil.decrypt(properties.getProperty("database.url"), encryptionKey);
            String dbUsername = EncryptionUtil.decrypt(properties.getProperty("database.username"), encryptionKey);
            String dbPassword = EncryptionUtil.decrypt(properties.getProperty("database.password"), encryptionKey);

            dbConnection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            logger.info("Connected to the database: {}", dbUrl);
        } catch (IOException | SQLException e) {
            logger.error("Failed to connect to the database: {}", e.getMessage());
        }
    }

    public void saveUrlToDatabase(String url, String content) {
        try {
            Document htmlDocument = Jsoup.parse(content);
            String headerText = htmlDocument.select("header").text();
            String bodyText = htmlDocument.select("body").text();

            String content1 = headerText + "\n" + bodyText;

            PreparedStatement preparedStatement = dbConnection.prepareStatement(
                    "INSERT INTO crawledurls (url, content) VALUES (?, ?)");
            preparedStatement.setString(1, url);
            preparedStatement.setString(2, content1);
            preparedStatement.executeUpdate();
            preparedStatement.close();

            logger.info("Saved URL to the database: {}", url);
        } catch (SQLException e) {
            logger.error("Error saving URL to the database: {}", e.getMessage());
        }
    }

    public void buildInvertedIndex(String url, String content) {
        String[] words = content.toLowerCase().split("\\s+");
        ConcurrentMap<String, Integer> termFrequency = new ConcurrentHashMap<>();


        for (String word : words) {
            termFrequency.put(word, termFrequency.getOrDefault(word, 0) + 1);
        }

        for (String term : termFrequency.keySet()) {
            invertedIndex.putIfAbsent(term, new ConcurrentHashMap<>());
            invertedIndex.get(term).put(url, termFrequency.get(term));
        }
    }

    public void calculateTFIDF() {
        for (String term : invertedIndex.keySet()) {
            ConcurrentMap<String, Integer> docFrequency = invertedIndex.get(term);
            int df = docFrequency.size();
            double idf = Math.log((double) numDocuments / (df + 1)) + 1;
            
            for (String url : docFrequency.keySet()) {
                int tf = docFrequency.get(url);
                double tfidf = tf * idf;
                docFrequency.put(url, (int) tfidf);
            }
        }
    }

    public List<String> searchDocuments(String query) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        ConcurrentMap<String, Integer> queryTermFrequency = new ConcurrentHashMap<>();

        for (String term : queryTerms) {
            queryTermFrequency.put(term, queryTermFrequency.getOrDefault(term, 0) + 1);
        }

        ConcurrentMap<String, Double> urlScores = new ConcurrentHashMap<>();
        for (String term : queryTermFrequency.keySet()) {
            ConcurrentMap<String, Integer> docFrequency = invertedIndex.get(term);
            if (docFrequency != null) {
                int df = docFrequency.size();
                double idf = Math.log((double) numDocuments / (df + 1)) + 1;
                int tf_query = queryTermFrequency.get(term);
                double tfidf_query = tf_query * idf;

                for (String url : docFrequency.keySet()) {
                    int tf_doc = docFrequency.get(url);
                    double tfidf_doc = tf_doc * idf;
                    urlScores.put(url, urlScores.getOrDefault(url, 0.0) + tfidf_query * tfidf_doc);
                }
            }
        }

        List<String> relevantUrls = new ArrayList<>();
        urlScores.entrySet().stream()
                .sorted(ConcurrentMap.Entry.<String, Double>comparingByValue().reversed())
                .limit(10) // Limiting the results to the top 10 relevant URLs
                .forEach(entry -> relevantUrls.add(entry.getKey()));

        return relevantUrls;
    }

    public void crawl(String startingUrl, int maxPages, long maxTime, long delayDuration) {
        queue.add(startingUrl);
        int pageCount = 0;

        long startTime = System.currentTimeMillis();
        while (!queue.isEmpty() && pageCount < maxPages && (System.currentTimeMillis() - startTime) <= maxTime) {
            String url = queue.poll();

            if (!visitedUrls.contains(url)) {
                // Check if the URL is empty or null
                if (url == null || url.trim().isEmpty()) {
                    logger.warn("Skipping empty URL: {}", url);
                    continue;
                }

                logger.info("Crawling: {}", url);
                visitedUrls.add(url);
                pageCount++;

                try {
                    Connection connection = Jsoup.connect(url);
                    Document htmlDocument = connection.get();
                    Elements linksOnPage = htmlDocument.select("a[href]");

                    for (Element link : linksOnPage) {
                        String nextUrl = link.absUrl("href");
                        if (!nextUrl.isEmpty() && !visitedUrls.contains(nextUrl)) {
                            queue.add(nextUrl);
                        } else {
                            logger.warn("Skipping empty or dead URL: {}", nextUrl);
                        }
                    }

                    // Extract the content of the URL's body
                    String content = htmlDocument.html();

                    // Save the crawled URL to the database
                    saveUrlToDatabase(url, content);

                    // Build inverted index and update term frequency and document frequency
                    buildInvertedIndex(url, content);
                    numDocuments++;

                } catch (IOException e) {
                    logger.error("Error connecting to: {}", url);
                }

                // Add delay between requests
                try {
                    Thread.sleep(delayDuration);
                } catch (InterruptedException e) {
                    logger.error("Thread sleep interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }

        logger.info("Crawling finished!");
        calculateTFIDF();
    }
    
    private String calculateUrlHash(String url) {
        return Hashing.sha256()
                .hashString(url, StandardCharsets.UTF_8)
                .toString();
    }

    
    public void saveInvertedIndexToDatabase() {
        try {
            PreparedStatement preparedStatement = dbConnection.prepareStatement(
                "INSERT INTO invertedindex (term, url_Hash,url, frequency) VALUES (?, ?, ?, ?)" + "ON DUPLICATE KEY UPDATE frequency = VALUES(frequency)");

            for (String term : invertedIndex.keySet()) {
                ConcurrentMap<String, Integer> docFrequency = invertedIndex.get(term);

                for (String url : docFrequency.keySet()) {
                    int frequency = docFrequency.get(url);
                    
                    // Use the URL's hash as the primary key for the inverted index
                    String urlHash = calculateUrlHash(url);
                    
                    preparedStatement.setString(1, term);
                    preparedStatement.setString(2, urlHash);
                    preparedStatement.setString(3, url);
                    preparedStatement.setInt(4, frequency);
                    preparedStatement.addBatch(); // Add the prepared statement to the batch
                }
            }

            // Execute the batch insert
            preparedStatement.executeBatch();
            preparedStatement.close();

            logger.info("Inverted index saved to the database.");
        } catch (SQLException e) {
            logger.error("Error saving inverted index to the database: {}", e.getMessage());
        }
    }


    public List<String> readStartingUrlsFromFile(String filePath) throws IOException {
        List<String> startingUrls = new ArrayList<>();
        Path path = Paths.get(filePath);
        startingUrls = Files.readAllLines(path);
        return startingUrls;
    }

    public static void main(String[] args) {
        System.out.println("Program started");
        WebCrawler webCrawler = new WebCrawler();

        // Add code for database connection properties, encryption, and other configurations here
        String encryptedPropertiesFile = "C:\\Users\\Mahesh Thakur\\Desktop\\CrawlerFolder\\Crawler2.properties.encrypted";

        Scanner scanner = new Scanner(System.in);
        logger.info("Enter the encryption key: ");
        String encryptionKey = scanner.nextLine();

        webCrawler.connectionToDatabase(encryptedPropertiesFile, encryptionKey);


        logger.info("Enter the path to the file containing the starting URLs: ");
        String filePath = scanner.nextLine();

        List<String> startingUrls;
        try {
            startingUrls = webCrawler.readStartingUrlsFromFile(filePath);
        } catch (IOException e) {
            logger.error("Error reading starting URLs from file: {}", e.getMessage());
            return;
        }
        
        int threadNum = startingUrls.size();
        logger.info("Using {} threads: ", threadNum);

//        // Check if the number of starting URLs matches the number of threads
//        if (startingUrls.size() != threadNum) {
//            logger.error("The number of starting URLs provided does not match the number of threads!");
//            return;
//        }

        logger.info("Enter the maximum number of pages to crawl: ");
        int maxPages = scanner.nextInt();
        logger.info("Enter the maximum time (in milliseconds) to crawl: ");
        long maxTime = scanner.nextLong();
        logger.info("Enter the delay duration (in milliseconds) between requests: ");
        long delayDuration = scanner.nextInt();
        scanner.nextLine();

        webCrawler.executorService = Executors.newFixedThreadPool(threadNum); // Assign executorService

        for (int i = 0; i < threadNum; i++) {
            String startingUrl = startingUrls.get(i);
            webCrawler.executorService.execute(() -> {
                webCrawler.crawl(startingUrl, maxPages, maxTime, delayDuration);
            });
        }

        // Wait for the crawling to finish
        try {
            webCrawler.executorService.shutdown();
            webCrawler.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS); // Wait for all threads to finish
            
            // Save the inverted index to the database
            webCrawler.saveInvertedIndexToDatabase();
            
            webCrawler.dbConnection.close(); // Close the database connection here
            LogManager.shutdown();
        } catch (SQLException e) {
            logger.error("Failed to close the database connection: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting for executor service termination: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        // Now that crawling is finished, provide search functionality to the user
        scanner.nextLine(); // Consume the newline character left by nextInt()

        while (true) {
            System.out.println("Enter your search query (type 'exit' to quit): ");
            String searchQuery = scanner.nextLine().toLowerCase();
            if (searchQuery.equals("exit")) {
                break;
            }

            // Implement search logic here based on the searchQuery and the inverted index
            List<String> relevantUrls = webCrawler.searchDocuments(searchQuery);
            System.out.println("Relevant URLs for your search query:");
            for (String url : relevantUrls) {
                System.out.println(url);
            }
        }

        scanner.close();
    }
}

        