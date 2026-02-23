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

public class WikipediaProvider implements SearchProvider{
    
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "Wikipedia";
    }

    @Override
    public List<SearchResult> search(SearchQuery query) throws ProviderException {
        try {
            String encoded = URLEncoder.encode(query.getText(), StandardCharsets.UTF_8);

            String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srsearch=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("User-Agent", "MAArK-Privacy-Search-Engine (student project)")
                                .GET()
                                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ProviderException("Wikipedia returned status: " + response.statusCode());
            }

            String body = response.body();

            if (!body.trim().startsWith("{")) {
                throw new ProviderException("Unexpected response from Wikipedia.");
            }

            JsonNode root = mapper.readTree(body);
            JsonNode searchArray = root.path("query").get("search");

            List<SearchResult> results = new ArrayList<>();

            for(JsonNode item: searchArray) {
                String title = item.get("title").asText();
                String snippet = item.get("snippet").asText();
                String link = "https://en.wikipedia.org/wiki/" + title.replace(" ", "_");

                results.add(new SearchResult(title, link, snippet, getName()));
            }

            return results;
        } catch (Exception e){
            throw new ProviderException("Wikipedia search failed: " + e.getMessage());
        }
    }
}
