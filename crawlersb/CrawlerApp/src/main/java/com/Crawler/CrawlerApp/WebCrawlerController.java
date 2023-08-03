package com.Crawler.CrawlerApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(allowCredentials = "true", origins = "http://127.0.0.1:5500")
public class WebCrawlerController {

    private final WebCrawler webCrawler;

    @Autowired
    public WebCrawlerController(WebCrawler webCrawler) {
        this.webCrawler = webCrawler;
    }

    @PostMapping("/api/fetch-content")
    public ResponseEntity<String> fetchContent(@RequestParam String url) {
        // Fetch the content of the URL using the WebCrawler service
        String content = webCrawler.fetchHtmlContent(url);
        if (content != null) {
            return new ResponseEntity<>(content, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
