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

public class StackOverflowProvider implements SearchProvider {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "StackOverflow";
    }

    @Override
    public List<SearchResult> search(SearchQuery query) throws ProviderException {
        try {
            String encoded = URLEncoder.encode(query.getText(), StandardCharsets.UTF_8);
            String url = "https://api.stackexchange.com/2.3/search?order=desc&sort=relevance&site=stackoverflow&intitle="
                    + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "MAArK-Privacy-Search-Engine (student project)")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ProviderException("StackOverflow returned status: " + response.statusCode());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode items = root.path("items");

            List<SearchResult> results = new ArrayList<>();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String title = item.path("title").asText("");
                    String link = item.path("link").asText("");
                    results.add(new SearchResult(title, link, title, getName()));
                }
            }

            return results;
        } catch (Exception e) {
            throw new ProviderException("StackOverflow search failed: " + e.getMessage());
        }
    }
}
