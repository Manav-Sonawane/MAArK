package com.maark.provider;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartpageProvider implements SearchProvider {

    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "<a class=\"w-gl__result-title result-link\" href=\"([^\"]+)\"([^>]*)>([^<]*)</a>");
    private static final Pattern SNIPPET_PATTERN = Pattern.compile(
            "<p class=\"w-gl__description\">([^<]*)</p>");

    @Override
    public String getName() {
        return "Startpage";
    }

    @Override
    public List<SearchResult> search(SearchQuery query) throws ProviderException {
        System.out.println("Provider: " + getName() + " started");
        try {
            String encoded = URLEncoder.encode(query.getText(), StandardCharsets.UTF_8);
            String url = "https://www.startpage.com/sp/search?query=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();

            // Use the shared client
            HttpResponse<String> response = SearchContext.CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Startpage returned status: " + response.statusCode());
                return new ArrayList<>(); // Return empty lightly instead of crashing, as it might block scraping
            }

            String body = response.body();
            List<SearchResult> results = new ArrayList<>();

            Matcher titleMatcher = RESULT_PATTERN.matcher(body);
            Matcher snippetMatcher = SNIPPET_PATTERN.matcher(body);

            while (titleMatcher.find()) {
                String link = titleMatcher.group(1);
                String title = titleMatcher.group(3).trim();
                String snippet = "";
                if (snippetMatcher.find()) {
                    snippet = snippetMatcher.group(1).trim();
                }

                if (!link.isEmpty() && !title.isEmpty()) {
                    // Quick HTML entity unescape (basic)
                    title = title.replace("&amp;", "&").replace("&quot;", "\"").replace("&#x27;", "'");
                    snippet = snippet.replace("&amp;", "&").replace("&quot;", "\"").replace("&#x27;", "'");
                    
                    results.add(new SearchResult(title, link, snippet, getName()));
                }
            }

            return results;
        } catch (Exception e) {
            throw new ProviderException("Startpage search failed: " + e.getMessage());
        }
    }
}
