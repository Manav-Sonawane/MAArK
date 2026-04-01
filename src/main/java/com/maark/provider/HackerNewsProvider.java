package com.maark.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.maark.exception.ProviderException;
import com.maark.model.SearchQuery;
import com.maark.model.SearchResult;
import com.maark.util.SearchContext;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HackerNewsProvider implements SearchProvider {

    @Override
    public String getName() {
        return "HackerNews";
    }

    @Override
    public List<SearchResult> search(SearchQuery query) throws ProviderException {
        System.out.println("Provider: " + getName() + " started");
        try {
            String encoded = URLEncoder.encode(query.getText(), StandardCharsets.UTF_8);
            String url = "https://hn.algolia.com/api/v1/search?query=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "MAArK-Privacy-Search-Engine (student project)")
                    .GET()
                    .build();

            HttpResponse<String> response = SearchContext.CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ProviderException("HackerNews returned status: " + response.statusCode());
            }

            JsonNode root = SearchContext.MAPPER.readTree(response.body());
            JsonNode hits = root.path("hits");

            List<SearchResult> results = new ArrayList<>();

            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    String title = hit.path("title").asText("");
                    if (title.isEmpty()) {
                        title = hit.path("story_title").asText("");
                    }
                    String link = hit.path("url").asText("");
                    if (link.isEmpty()) {
                        link = "https://news.ycombinator.com/item?id=" + hit.path("objectID").asText("");
                    }

                    results.add(new SearchResult(title, link, title, getName()));
                }
            }

            return results;
        } catch (Exception e) {
            throw new ProviderException("HackerNews search failed: " + e.getMessage());
        }
    }
}
