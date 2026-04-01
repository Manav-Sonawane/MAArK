package com.maark.model;

public class SearchHistoryEntry {
    private String query;
    private long timestamp;

    public SearchHistoryEntry() {}

    public SearchHistoryEntry(String query, long timestamp) {
        this.query = query;
        this.timestamp = timestamp;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
