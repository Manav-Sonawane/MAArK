package com.maark.ui;

import com.maark.controller.SearchController;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class MaarkApp extends Application {

    private boolean isDarkMode = false;
    private Scene scene;
    private BorderPane root;
    private TextField searchField;
    private Button searchBtn;
    private ListView<String> resultsList;
    private Label statusLabel;
    private ToggleButton themeToggle;
    private WebView webView;
    private VBox resultsPane;
    private SearchController controller;

    @Override
    public void start(Stage stage) {
        Button backBtn = new Button("◀");
        Button forwardBtn = new Button("▶");
        Button reloadBtn = new Button("↻");
        Button homeBtn = new Button("🏠");

        backBtn.getStyleClass().add("nav-btn");
        forwardBtn.getStyleClass().add("nav-btn");
        reloadBtn.getStyleClass().add("nav-btn");
        homeBtn.getStyleClass().add("nav-btn");

        themeToggle = new ToggleButton("🌙");
        themeToggle.getStyleClass().add("theme-toggle");
        themeToggle.setOnAction(e -> toggleTheme());

        searchField = new TextField();
        searchField.setPromptText("Search or enter address");
        searchField.setPrefHeight(35);
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchBtn = new Button("🔍");
        searchBtn.setPrefHeight(35);
        searchBtn.getStyleClass().add("search-button");

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10));
        toolbar.getStyleClass().add("toolbar");
        toolbar.getChildren().addAll(backBtn, forwardBtn, reloadBtn, homeBtn, searchField, searchBtn, themeToggle);

        resultsList = new ListView<>();
        resultsList.getStyleClass().add("results-list");

        resultsPane = new VBox(resultsList);
        resultsPane.setVisible(false);
        resultsPane.setManaged(false);
        resultsPane.getStyleClass().add("results-pane");
        VBox.setVgrow(resultsList, Priority.ALWAYS);

        webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        statusLabel = new Label("Ready.");
        statusLabel.getStyleClass().add("status-label");

        BorderPane bottomBar = new BorderPane();
        bottomBar.setLeft(statusLabel);
        bottomBar.setPadding(new Insets(2, 5, 2, 5));
        bottomBar.getStyleClass().add("bottom-bar");

        controller = new SearchController(resultsList, statusLabel, webEngine) {
            @Override
            public void handleSearch(String query) {
                if (query.startsWith("http://") || query.startsWith("https://")) {
                    webEngine.load(query);
                    hideResults();
                } else if (!query.isEmpty() && query.contains(".") && !query.contains(" ")) {
                    webEngine.load("https://" + query);
                    hideResults();
                } else {
                    super.handleSearch(query);
                    showResults();
                }
            }
        };

        searchBtn.setOnAction(e -> controller.handleSearch(searchField.getText()));

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                controller.handleSearch(searchField.getText());
            }
        });

        backBtn.setOnAction(e -> {
            try {
                if (webEngine.getHistory().getCurrentIndex() > 0) {
                    webEngine.getHistory().go(-1);
                    hideResults();
                }
            } catch (Exception ex) {
            }
        });
        forwardBtn.setOnAction(e -> {
            try {
                if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
                    webEngine.getHistory().go(1);
                    hideResults();
                }
            } catch (Exception ex) {
            }
        });
        reloadBtn.setOnAction(e -> {
            webEngine.reload();
            hideResults();
        });
        homeBtn.setOnAction(e -> {
            webEngine.loadContent(
                    "<html><body style='font-family:sans-serif;text-align:center;padding-top:100px;'><h1>MAArK Browser</h1><p>Privacy First Search Engine</p></body></html>");
            searchField.clear();
            hideResults();
        });

        resultsList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                String selectedResult = resultsList.getSelectionModel().getSelectedItem();
                if (selectedResult != null) {
                    String url = controller.extractUrl(selectedResult);
                    if (url != null && !url.isEmpty()) {
                        webEngine.load(url);
                        hideResults();
                    }
                }
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading: " + webEngine.getLocation());
                if (!webEngine.getLocation().isEmpty() && !webEngine.getLocation().startsWith("data:text/html")) {
                    searchField.setText(webEngine.getLocation());
                }
            } else if (newState == Worker.State.SUCCEEDED) {
                statusLabel.setText("Done.");
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load: " + webEngine.getLocation());
            }
        });

        StackPane centerPane = new StackPane();
        centerPane.getChildren().addAll(webView, resultsPane);
        StackPane.setAlignment(resultsPane, Pos.TOP_CENTER);
        resultsPane.setMaxWidth(800);
        resultsPane.setMaxHeight(500);
        StackPane.setMargin(resultsPane, new Insets(0, 0, 0, 0));

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(centerPane);
        root.setBottom(bottomBar);

        scene = new Scene(root, 1200, 700);
        applyLightTheme();

        homeBtn.fire();

        stage.setTitle("MAArK Browser");
        stage.setScene(scene);
        stage.show();
    }

    private void showResults() {
        resultsPane.setVisible(true);
        resultsPane.setManaged(true);
    }

    private void hideResults() {
        resultsPane.setVisible(false);
        resultsPane.setManaged(false);
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            applyDarkTheme();
            themeToggle.setText("☀️");
        } else {
            applyLightTheme();
            themeToggle.setText("🌙");
        }
    }

    private void applyLightTheme() {
        scene.getStylesheets().clear();
        String stylesheet = getClass().getResource("/styles/light-theme.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
    }

    private void applyDarkTheme() {
        scene.getStylesheets().clear();
        String stylesheet = getClass().getResource("/styles/dark-theme.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
