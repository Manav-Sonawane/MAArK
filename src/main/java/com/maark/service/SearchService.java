package com.maark.service;

import com.maark.exception.SearchException;
import com.maark.model.SearchQuery;
import com.maark.model.SearchResult;
import com.maark.provider.SearchProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SearchService {
    private final List<SearchProvider> providers;
    
    // Shared ExecutorService reused for all searches
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    public SearchService(List<SearchProvider> providers) {
        this.providers = providers;
    }

    public List<SearchResult> search(String rawQuery) throws SearchException {
        SearchQuery query = new SearchQuery(rawQuery);

        try {
            List<CompletableFuture<List<SearchResult>>> futures = providers.stream()
                    .map(provider -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return provider.search(query);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, EXECUTOR)
                            .orTimeout(5, TimeUnit.SECONDS)
                            .exceptionally(ex -> new ArrayList<>()))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new SearchException("Search failed");
        }
    }
    
    public void shutdown() {
        if (!EXECUTOR.isShutdown()) {
            EXECUTOR.shutdown();
        }
    }
}
