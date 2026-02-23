package com.maark.controller;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import com.maark.model.SearchResult;
import com.maark.service.SearchService;
import com.maark.provider.WikipediaProvider;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class SearchController {
    
    private final ListView<String> resultsList;
    private final Label statusLabel;
    private final WebEngine webEngine;
    private final SearchService searchService;

    public SearchController(ListView<String> resultsList, Label statusLabel, WebEngine webEngine) {
        this.resultsList = resultsList;
        this.statusLabel = statusLabel;
        this.webEngine = webEngine;

        this.searchService = new SearchService(List.of(new WikipediaProvider()));
    }

    public void handleSearch(String query){
        Task<Void> task = new Task<Void>(){
            @Override
            protected Void call() {
                try {
                    Platform.runLater(() -> statusLabel.setText("Searching..."));

                    List<SearchResult> results = searchService.search(query);

                    Platform.runLater(() -> {
                        resultsList.getItems().setAll(
                            results.stream().map(Object::toString).toList()
                        );
                        statusLabel.setText("Results: " + results.size());
                    });
                } catch (Exception e){
                    Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    public String extractUrl(String resultString) {
        if (resultString == null || resultString.trim().isEmpty()) {
            return null;
        }

        String[] lines = resultString.split("\n");
        if (lines.length >= 2) {
            return lines[1].trim();
        }

        return null;
    }
}
