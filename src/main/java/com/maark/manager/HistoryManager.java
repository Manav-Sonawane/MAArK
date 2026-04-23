package com.maark.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.maark.model.BrowseHistoryEntry;
import com.maark.model.SearchHistoryEntry;
import com.maark.util.SearchContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {

    private static final Path SEARCH_HISTORY_FILE = Paths.get(System.getProperty("user.dir"), "search_history.json");
    private static final Path BROWSE_HISTORY_FILE = Paths.get(System.getProperty("user.dir"), "browse_history.json");
    private static final int MAX_ENTRIES = 100;

    private List<SearchHistoryEntry> searchHistory;
    private List<BrowseHistoryEntry> browseHistory;

    public HistoryManager() {
        searchHistory = new ArrayList<>();
        browseHistory = new ArrayList<>();
        loadHistory();
    }

    private synchronized void loadHistory() {
        try {
            File searchFile = SEARCH_HISTORY_FILE.toFile();
            if (searchFile.exists() && searchFile.length() > 0) {
                try {
                    searchHistory = SearchContext.MAPPER.readValue(searchFile, new TypeReference<List<SearchHistoryEntry>>() {});
                    System.out.println("Loaded search history: " + searchHistory.size());
                } catch (Exception e) {
                    System.err.println("Failed to parse search history, initializing empty list.");
                    searchHistory = new ArrayList<>();
                }
            } else {
                searchHistory = new ArrayList<>();
            }

            File browseFile = BROWSE_HISTORY_FILE.toFile();
            if (browseFile.exists() && browseFile.length() > 0) {
                try {
                    browseHistory = SearchContext.MAPPER.readValue(browseFile, new TypeReference<List<BrowseHistoryEntry>>() {});
                    System.out.println("Loaded browse history: " + browseHistory.size());
                } catch (Exception e) {
                    System.err.println("Failed to parse browse history, initializing empty list.");
                    browseHistory = new ArrayList<>();
                }
            } else {
                browseHistory = new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (searchHistory == null) searchHistory = new ArrayList<>();
            if (browseHistory == null) browseHistory = new ArrayList<>();
        }
    }

    private synchronized void saveSearchHistory() {
        try {
            SearchContext.MAPPER.writeValue(SEARCH_HISTORY_FILE.toFile(), searchHistory);
            System.out.println("Saved search history: " + searchHistory.size() + " entries.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveBrowseHistory() {
        try {
            SearchContext.MAPPER.writeValue(BROWSE_HISTORY_FILE.toFile(), browseHistory);
            System.out.println("Saved browse history: " + browseHistory.size() + " entries.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addSearch(String query) {
        searchHistory.add(0, new SearchHistoryEntry(query, System.currentTimeMillis()));
        if (searchHistory.size() > MAX_ENTRIES) {
            searchHistory = new ArrayList<>(searchHistory.subList(0, MAX_ENTRIES));
        }
        saveSearchHistory();
    }

    public synchronized void addBrowse(String url, String title) {
        browseHistory.add(0, new BrowseHistoryEntry(url, title, System.currentTimeMillis()));
        if (browseHistory.size() > MAX_ENTRIES) {
            browseHistory = new ArrayList<>(browseHistory.subList(0, MAX_ENTRIES));
        }
        saveBrowseHistory();
    }

    public synchronized List<SearchHistoryEntry> getRecentSearches(int limit) {
        int toIndex = Math.min(limit, searchHistory.size());
        return new ArrayList<>(searchHistory.subList(0, toIndex));
    }

    public synchronized List<BrowseHistoryEntry> getRecentBrowses(int limit) {
        int toIndex = Math.min(limit, browseHistory.size());
        return new ArrayList<>(browseHistory.subList(0, toIndex));
    }

    public synchronized void clearAll() {
        searchHistory.clear();
        browseHistory.clear();
        saveSearchHistory();
        saveBrowseHistory();
    }
}
