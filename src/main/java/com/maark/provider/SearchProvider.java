package com.maark.provider;

import com.maark.exception.ProviderException;
import com.maark.model.SearchQuery;
import com.maark.model.SearchResult;

import java.util.List;

public interface SearchProvider {
    List<SearchResult> search(SearchQuery query) throws ProviderException;
    String getName();
}
