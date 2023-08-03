package com.Crawler.CrawlerApp;

public class SearchRequest {
    private String query;
    private int page;
    private String[] queryTerms; // New field to store the individual query terms

    public SearchRequest() {
    }

    public SearchRequest(String query, int page, String[] queryTerms) {
        this.query = query;
        this.page = page;
        this.queryTerms = queryTerms;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String[] getQueryTerms() {
        return queryTerms;
    }

    public void setQueryTerms(String[] queryTerms) {
        this.queryTerms = queryTerms;
    }
}
