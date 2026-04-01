package com.maark.model;

public class BrowseHistoryEntry {
    private String url;
    private String title;
    private long timestamp;

    public BrowseHistoryEntry() {}

    public BrowseHistoryEntry(String url, String title, long timestamp) {
        this.url = url;
        this.title = title;
        this.timestamp = timestamp;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
