package com.maark.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;

public class SearchContext {
    public static final HttpClient CLIENT = HttpClient.newHttpClient();
    public static final ObjectMapper MAPPER = new ObjectMapper();
}
