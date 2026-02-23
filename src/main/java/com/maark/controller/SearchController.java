package com.maark.controller;

import javafx.application.Platform;
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
    private final SearchService searchService;

    public SearchController(ListView<String> resultsList, Label statusLabel) {
        this.resultsList = resultsList;
        this.statusLabel = statusLabel;

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
}
