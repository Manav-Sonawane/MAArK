package com.maark.service;

import com.maark.exception.SearchException;
import com.maark.model.SearchQuery;
import com.maark.model.SearchResult;
import com.maark.provider.SearchProvider;

import java.util.ArrayList;
import java.util.List;

public class SearchService {
    private final List<SearchProvider> providers;

    public SearchService(List<SearchProvider> providers){
        this.providers = providers;
    }

    public List<SearchResult> search(String rawQuery) throws SearchException {
        SearchQuery query = new SearchQuery(rawQuery);

        List<SearchResult> combined = new ArrayList<>();

        for(SearchProvider provider: providers){
            combined.addAll(provider.search(query));
        }
        return combined;
    }
}
