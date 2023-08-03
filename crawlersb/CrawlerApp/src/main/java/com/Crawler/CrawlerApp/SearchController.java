 package com.Crawler.CrawlerApp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin(allowCredentials = "true", origins = "http://127.0.0.1:5500")
public class SearchController {
	
	private static final Logger logger = LogManager.getLogger(SearchController.class);

    private final WebCrawler webCrawler;

    @Autowired
    public SearchController(WebCrawler webCrawler) {
        this.webCrawler = webCrawler;
    }

    @PostMapping("/api/search")
    public SearchResponse search(@RequestBody SearchRequest request) {
    	 logger.info("Received search request: {}", request); // Log the received search request
        webCrawler.connectionToDatabase();
        List<String> relevantUrls = webCrawler.searchDocuments(request.getQuery());
        webCrawler.closeDatabaseConnection();
        logger.info("Relevant URLs returned from the search: {}", relevantUrls);

//        // Calculate total number of pages and paginate the results
        int pageSize = 5;
        int totalResults = relevantUrls.size();
        int totalPages = (int) Math.ceil((double) totalResults / pageSize);

        String[] queryTerms = request.getQuery().toLowerCase().replaceAll("^query=", "").split("\\s+|\\+");
        logger.info("Query terms: {}", Arrays.toString(queryTerms));

        return new SearchResponse(relevantUrls, 1, totalPages, queryTerms);
    }
}