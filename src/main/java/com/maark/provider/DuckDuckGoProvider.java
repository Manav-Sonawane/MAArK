package com.maark.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maark.exception.ProviderException;
import com.maark.model.SearchQuery;
import com.maark.model.SearchResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DuckDuckGoProvider implements SearchProvider {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "DuckDuckGo";
    }

    @Override
    public List<SearchResult> search(SearchQuery query) throws ProviderException {
        try {
            String encoded = URLEncoder.encode(query.getText(), StandardCharsets.UTF_8);
            String url = "https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_redirect=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "MAArK-Privacy-Search-Engine (student project)")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ProviderException("DuckDuckGo returned status: " + response.statusCode());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode relatedTopics = root.path("RelatedTopics");

            List<SearchResult> results = new ArrayList<>();

            if (relatedTopics.isArray()) {
                for (JsonNode item : relatedTopics) {
                    if (item.has("FirstURL") && item.has("Text")) {
                        String link = item.get("FirstURL").asText();
                        String text = item.get("Text").asText();
                        results.add(new SearchResult(text, link, text, getName()));
                    }
                }
            }

            return results;
        } catch (Exception e) {
            throw new ProviderException("DuckDuckGo search failed: " + e.getMessage());
        }
    }
}
