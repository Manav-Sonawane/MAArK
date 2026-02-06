package com.maark.controller;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class SearchController {
    
    private final ListView<String> resultsList;
    private final Label statusLabel;

    public SearchController(ListView<String> resultsList, Label statusLabel) {
        this.resultsList = resultsList;
        this.statusLabel = statusLabel;
    }

    public void handleSearch(String rawQuery){
        String query = rawQuery == null ? "" : rawQuery.trim();

        if (query.isEmpty()){
            statusLabel.setText("Enter a search query.");
            return;
        }

        statusLabel.setText("Searching for: " + query);

        List<String> dummyResults = List.of(
            "Result 1: " + query + " - https://example.com",
            "Result 2: " + query + " - https://example.org",
            "Result 3: " + query + " - https://example.net"
        );

        Platform.runLater(()-> {
            resultsList.getItems().setAll(dummyResults);
            statusLabel.setText("Found " + dummyResults.size() + " results for: " + query);
        });
    }
}
