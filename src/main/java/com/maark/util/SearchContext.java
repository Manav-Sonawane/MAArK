package com.maark.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;

public class SearchContext {
    public static HttpClient CLIENT = HttpClient.newHttpClient();
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static void setClient(HttpClient newClient) {
        CLIENT = newClient;
    }
}
