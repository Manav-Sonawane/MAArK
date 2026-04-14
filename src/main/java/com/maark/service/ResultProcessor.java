package com.maark.service;

import com.maark.model.SearchResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles deduplication, scoring, and ranking of merged search results.
 */
public class ResultProcessor {

    // Source priority scores (higher = more trusted)
    private static final Map<String, Integer> SOURCE_PRIORITY = new HashMap<>();

    static {
        SOURCE_PRIORITY.put("local",       100);
        SOURCE_PRIORITY.put("wikipedia",    80);
        SOURCE_PRIORITY.put("stackoverflow", 70);
        SOURCE_PRIORITY.put("hackernews",   60);
        SOURCE_PRIORITY.put("duckduckgo",   50);
        SOURCE_PRIORITY.put("reddit",       40);
    }

    /**
     * Deduplicate, score, and sort results.
     *
     * @param rawResults merged results from all sources
     * @param query      original user query
     * @return ranked, deduplicated list
     */
    public List<SearchResult> process(List<SearchResult> rawResults, String query) {
        List<SearchResult> deduped = deduplicate(rawResults);
        return rank(deduped, query);
    }

    // ── Deduplication ─────────────────────────────────────────────────────────

    private List<SearchResult> deduplicate(List<SearchResult> results) {
        Map<String, SearchResult> seen = new LinkedHashMap<>();

        for (SearchResult r : results) {
            // Primary key: normalized URL
            String urlKey = normalizeUrl(r.getUrl());
            if (!urlKey.isEmpty() && !seen.containsKey(urlKey)) {
                seen.put(urlKey, r);
                continue;
            }

            // Fallback: normalized title
            String titleKey = "title:" + normalizeTitle(r.getTitle());
            if (!seen.containsKey(titleKey)) {
                seen.put(urlKey.isEmpty() ? titleKey : urlKey, r);
            }
        }

        return new ArrayList<>(seen.values());
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return "";
        return url.trim()
                .toLowerCase()
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "")
                .replaceAll("/$", "");
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) return "";
        return title.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    // ── Ranking ───────────────────────────────────────────────────────────────

    private List<SearchResult> rank(List<SearchResult> results, String query) {
        String lowerQuery = query == null ? "" : query.toLowerCase();

        return results.stream()
                .sorted(Comparator.comparingInt(
                        (SearchResult r) -> score(r, lowerQuery)).reversed())
                .collect(Collectors.toList());
    }

    private int score(SearchResult r, String lowerQuery) {
        int score = 0;

        // 1. Source priority
        String src = r.getSource() == null ? "" : r.getSource().toLowerCase();
        score += SOURCE_PRIORITY.getOrDefault(src, 30);

        if (lowerQuery.isEmpty()) return score;

        // 2. Title exact match
        String title = r.getTitle() == null ? "" : r.getTitle().toLowerCase();
        if (title.equals(lowerQuery)) {
            score += 60;
        } else if (title.contains(lowerQuery)) {
            score += 40;
        } else {
            // Partial word match in title
            String[] words = lowerQuery.split("\\s+");
            long titleMatches = Arrays.stream(words).filter(title::contains).count();
            score += (int) (titleMatches * 10);
        }

        // 3. Snippet/content match
        String snippet = r.getSnippet() == null ? "" : r.getSnippet().toLowerCase();
        if (snippet.contains(lowerQuery)) {
            score += 20;
        } else {
            String[] words = lowerQuery.split("\\s+");
            long snippetMatches = Arrays.stream(words).filter(snippet::contains).count();
            score += (int) (snippetMatches * 5);
        }

        return score;
    }
}
