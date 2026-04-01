package com.maark.local;

public class LocalDocument {
    private final String title;
    private final String content;
    private final String url;

    public LocalDocument(String title, String content, String url) {
        this.title = title;
        this.content = content;
        this.url = url;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getUrl() { return url; }
}
