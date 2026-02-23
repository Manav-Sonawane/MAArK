package com.maark.model;

public class SearchQuery {
    private final String text;

    public SearchQuery(String text){
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
