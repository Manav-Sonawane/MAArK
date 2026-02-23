package com.maark.model;

public class SearchResult {
    
    private String title;
    private String url;
    private String snippet;
    private String source;

    public SearchResult(String title, String url, String snippet, String source){
        this.title = title;
        this.url = url;
        this.snippet = snippet;
        this.source = source;
    }

    @Override
    public String toString() {
        return title + " [" + source + "]\n" + url + "\n" + snippet;
    }
}
