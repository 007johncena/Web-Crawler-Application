package com.Crawler.CrawlerApp;

import java.util.List;

public class SearchResponse {
    private List<String> results;
    private int currentPage;
    private int totalPages;
    private String[] queryTerms; // New field to store the query terms

    public SearchResponse() {
    }

    public SearchResponse(List<String> results, int currentPage, int totalPages, String[] queryTerms) {
        this.results = results;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.queryTerms = queryTerms;
    }

    public List<String> getResults() {
        return results;
    }

    public void setResults(List<String> results) {
        this.results = results;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public String[] getQueryTerms() {
        return queryTerms;
    }

    public void setQueryTerms(String[] queryTerms) {
        this.queryTerms = queryTerms;
    }
}
