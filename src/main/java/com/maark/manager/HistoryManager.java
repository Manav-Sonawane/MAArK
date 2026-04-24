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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HistoryManager handles multi-profile history isolation.
 * Each profile (Personal, Work, Guest, etc.) gets its own persisted JSON file.
 */
public class HistoryManager {

    private static final int MAX_ENTRIES = 100;
    
    // Memory cache: profileName -> history list
    private final Map<String, List<SearchHistoryEntry>> searchHistories = new HashMap<>();
    private final Map<String, List<BrowseHistoryEntry>> browseHistories = new HashMap<>();

    public HistoryManager() {
        // Histories are loaded lazily when requested for a profile
    }

    private synchronized void ensureLoaded(String profile) {
        if (!searchHistories.containsKey(profile)) {
            searchHistories.put(profile, loadSearchHistory(profile));
        }
        if (!browseHistories.containsKey(profile)) {
            browseHistories.put(profile, loadBrowseHistory(profile));
        }
    }

    private List<SearchHistoryEntry> loadSearchHistory(String profile) {
        File file = getSearchFile(profile);
        if (file.exists() && file.length() > 0) {
            try {
                return SearchContext.MAPPER.readValue(file, new TypeReference<List<SearchHistoryEntry>>() {});
            } catch (Exception e) {
                System.err.println("Failed to parse search history for " + profile);
            }
        }
        return new ArrayList<>();
    }

    private List<BrowseHistoryEntry> loadBrowseHistory(String profile) {
        File file = getBrowseFile(profile);
        if (file.exists() && file.length() > 0) {
            try {
                return SearchContext.MAPPER.readValue(file, new TypeReference<List<BrowseHistoryEntry>>() {});
            } catch (Exception e) {
                System.err.println("Failed to parse browse history for " + profile);
            }
        }
        return new ArrayList<>();
    }

    private synchronized void saveSearchHistory(String profile) {
        try {
            SearchContext.MAPPER.writeValue(getSearchFile(profile), searchHistories.get(profile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveBrowseHistory(String profile) {
        try {
            SearchContext.MAPPER.writeValue(getBrowseFile(profile), browseHistories.get(profile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getSearchFile(String profile) {
        String clean = profile.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return Paths.get(System.getProperty("user.dir"), clean + "_search_history.json").toFile();
    }

    private File getBrowseFile(String profile) {
        String clean = profile.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return Paths.get(System.getProperty("user.dir"), clean + "_browse_history.json").toFile();
    }

    public synchronized void addSearch(String profile, String query) {
        ensureLoaded(profile);
        List<SearchHistoryEntry> history = searchHistories.get(profile);
        history.add(0, new SearchHistoryEntry(query, System.currentTimeMillis()));
        if (history.size() > MAX_ENTRIES) {
            searchHistories.put(profile, new ArrayList<>(history.subList(0, MAX_ENTRIES)));
        }
        saveSearchHistory(profile);
    }

    public synchronized void addBrowse(String profile, String url, String title) {
        ensureLoaded(profile);
        List<BrowseHistoryEntry> history = browseHistories.get(profile);
        history.add(0, new BrowseHistoryEntry(url, title, System.currentTimeMillis()));
        if (history.size() > MAX_ENTRIES) {
            browseHistories.put(profile, new ArrayList<>(history.subList(0, MAX_ENTRIES)));
        }
        saveBrowseHistory(profile);
    }

    public synchronized List<SearchHistoryEntry> getRecentSearches(String profile, int limit) {
        ensureLoaded(profile);
        List<SearchHistoryEntry> history = searchHistories.get(profile);
        int toIndex = Math.min(limit, history.size());
        return new ArrayList<>(history.subList(0, toIndex));
    }

    public synchronized List<BrowseHistoryEntry> getRecentBrowses(String profile, int limit) {
        ensureLoaded(profile);
        List<BrowseHistoryEntry> history = browseHistories.get(profile);
        int toIndex = Math.min(limit, history.size());
        return new ArrayList<>(history.subList(0, toIndex));
    }

    public synchronized void clearAll(String profile) {
        ensureLoaded(profile);
        searchHistories.get(profile).clear();
        browseHistories.get(profile).clear();
        saveSearchHistory(profile);
        saveBrowseHistory(profile);
    }
}
