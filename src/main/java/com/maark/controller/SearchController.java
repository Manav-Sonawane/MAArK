package com.maark.controller;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import com.maark.model.SearchResult;
import com.maark.service.SearchService;
import com.maark.manager.HistoryManager;
import com.maark.provider.DuckDuckGoProvider;
import com.maark.provider.HackerNewsProvider;
import com.maark.provider.RedditProvider;
import com.maark.provider.StackOverflowProvider;
import com.maark.provider.WikipediaProvider;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;
import javafx.stage.Popup;
import javafx.geometry.Bounds;

import java.util.List;
import java.util.stream.Collectors;

public class SearchController {

    private final TextField searchField;
    private final ListView<String> suggestionList;
    private final ListView<SearchResult> resultsList;
    private final Label statusLabel;
    private final WebEngine webEngine;
    private final SearchService searchService;
    private final HistoryManager historyManager;
    private final Popup popup;

    public SearchController(TextField searchField, ListView<String> suggestionList, ListView<SearchResult> resultsList, Label statusLabel, WebEngine webEngine) {
        this.searchField = searchField;
        this.suggestionList = suggestionList;
        this.resultsList = resultsList;
        this.statusLabel = statusLabel;
        this.webEngine = webEngine;
        this.historyManager = new HistoryManager();

        this.searchService = new SearchService(List.of(
                new com.maark.provider.StartpageProvider(),
                new WikipediaProvider(),
                new DuckDuckGoProvider(),
                new StackOverflowProvider(),
                new RedditProvider(),
                new HackerNewsProvider()));
                
        this.popup = new Popup();
        this.popup.setAutoHide(true);
        this.popup.getContent().add(suggestionList);
        this.suggestionList.prefWidthProperty().bind(searchField.widthProperty());
                
        setupListView();
        setupSuggestions();
    }

    private void setupListView() {
        resultsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox();
                    Label titleLabel = new Label(result.getTitle());
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    Label sourceLabel = new Label(result.getSource());
                    sourceLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                    
                    Label snippetLabel = new Label(result.getSnippet());
                    snippetLabel.setStyle("-fx-font-size: 12px;");
                    snippetLabel.setWrapText(true);
                    
                    vbox.getChildren().addAll(titleLabel, sourceLabel, snippetLabel);
                    setGraphic(vbox);
                }
            }
        });
    }

    private void setupSuggestions() {
        searchField.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                updateAndShowSuggestions(searchField.getText());
            }
        });

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (searchField.isFocused()) {
                updateAndShowSuggestions(newText);
            }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                String selected = suggestionList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    searchField.setText(selected);
                    hideSuggestions();
                }
            }
        });

        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !suggestionList.isFocused()) {
                hideSuggestions();
            }
        });
    }

    private void updateAndShowSuggestions(String text) {
        String filter = text == null ? "" : text.toLowerCase();
        List<String> matches = historyManager.getRecentSearches(100).stream()
                .map(com.maark.model.SearchHistoryEntry::getQuery)
                .filter(query -> filter.isEmpty() || query.toLowerCase().contains(filter))
                .distinct()
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            hideSuggestions();
        } else {
            suggestionList.getItems().setAll(matches);
            if (!popup.isShowing() && searchField.getScene() != null && searchField.getScene().getWindow() != null) {
                Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
                if (bounds != null) {
                    popup.show(searchField, bounds.getMinX(), bounds.getMaxY());
                }
            }
        }
    }

    private void hideSuggestions() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    public void handleSearch(String query) {
        hideSuggestions();
        historyManager.addSearch(query);
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    Platform.runLater(() -> statusLabel.setText("Searching..."));

                    List<SearchResult> results = searchService.search(query);

                    Platform.runLater(() -> {
                        resultsList.getItems().setAll(results);
                        statusLabel.setText("Results: " + results.size());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(task).start();
    }
    
    public void handleResultClick(SearchResult result) {
        if (result != null) {
            historyManager.addBrowse(result.getUrl(), result.getTitle());
            webEngine.load(result.getUrl());
        }
    }

    public List<com.maark.model.SearchHistoryEntry> getRecentSearches(int limit) {
        return historyManager.getRecentSearches(limit);
    }
}
