package com.maark.service;

import com.maark.exception.SearchException;
import com.maark.local.LocalDocument;
import com.maark.local.LocalIndexer;
import com.maark.local.LocalSearchService;
import com.maark.local.SampleDataLoader;
import com.maark.model.SearchQuery;
import com.maark.model.SearchResult;
import com.maark.privacy.QueryEnhancer;
import com.maark.provider.SearchProvider;
import org.apache.lucene.store.Directory;

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

    // Local Lucene search — initialized once at startup
    private final LocalSearchService localSearchService;

    // Result post-processor: dedup + ranking
    private final ResultProcessor resultProcessor = new ResultProcessor();
    
    // Privacy query enhancer
    private final QueryEnhancer queryEnhancer = new QueryEnhancer(true);

    public SearchService(List<SearchProvider> providers) {
        this.providers = providers;
        this.localSearchService = initLocalSearch();
    }

    private LocalSearchService initLocalSearch() {
        try {
            Directory directory = LocalIndexer.createInMemoryDirectory();
            LocalIndexer indexer = new LocalIndexer(directory);
            List<LocalDocument> docs = SampleDataLoader.load();
            indexer.indexDocuments(docs);
            System.out.println("Local index built: " + docs.size() + " documents");
            return new LocalSearchService(directory, indexer.getAnalyzer());
        } catch (Exception e) {
            System.err.println("Failed to initialize local search index: " + e.getMessage());
            return null;
        }
    }

    public List<SearchResult> search(String rawQuery) throws SearchException {
        String enhancedQueryText = queryEnhancer.enhance(rawQuery);
        SearchQuery query = new SearchQuery(enhancedQueryText);

        try {
            // Provider futures (parallel)
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

            // Local search future (reuses same executor)
            CompletableFuture<List<SearchResult>> localFuture = localSearchService != null
                    ? CompletableFuture.supplyAsync(() -> localSearchService.search(rawQuery), EXECUTOR)
                            .orTimeout(3, TimeUnit.SECONDS)
                            .exceptionally(ex -> new ArrayList<>())
                    : CompletableFuture.completedFuture(new ArrayList<>());

            // Wait for all
            List<CompletableFuture<List<SearchResult>>> all = new ArrayList<>(futures);
            all.add(localFuture);
            CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).join();

            // Merge all raw results
            List<SearchResult> merged = new ArrayList<>(localFuture.join());
            futures.stream()
                    .map(CompletableFuture::join)
                    .forEach(merged::addAll);

            // Deduplicate → Rank → Return
            return resultProcessor.process(merged, rawQuery);

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
