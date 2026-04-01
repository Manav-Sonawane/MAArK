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

public class RedditProvider implements SearchProvider {

    @Override
    public String getName() {
        return "Reddit";
    }

    @Override
    public List<SearchResult> search(SearchQuery query) throws ProviderException {
        System.out.println("Provider: " + getName() + " started");
        try {
            String encoded = URLEncoder.encode(query.getText(), StandardCharsets.UTF_8);
            String url = "https://www.reddit.com/search.json?q=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "MAArK-Privacy-Search-Engine (student project)")
                    .GET()
                    .build();

            HttpResponse<String> response = SearchContext.CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ProviderException("Reddit returned status: " + response.statusCode());
            }

            JsonNode root = SearchContext.MAPPER.readTree(response.body());
            JsonNode children = root.path("data").path("children");

            List<SearchResult> results = new ArrayList<>();

            if (children.isArray()) {
                for (JsonNode child : children) {
                    JsonNode data = child.path("data");
                    String title = data.path("title").asText("");
                    String permalink = data.path("permalink").asText("");
                    String link = "https://www.reddit.com" + permalink;
                    String selftext = data.path("selftext").asText("");

                    if (selftext.length() > 200) {
                        selftext = selftext.substring(0, 200) + "...";
                    }

                    results.add(new SearchResult(title, link, selftext, getName()));
                }
            }

            return results;
        } catch (Exception e) {
            throw new ProviderException("Reddit search failed: " + e.getMessage());
        }
    }
}
