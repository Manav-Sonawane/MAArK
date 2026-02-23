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
    private VBox root;
    private Label title;
    private TextField searchField;
    private Button searchBtn;
    private ListView<String> resultsList;
    private Label statusLabel;
    private ToggleButton themeToggle;

    @Override
    public void start(Stage stage) {
        title = new Label("MAArK");
        title.getStyleClass().add("title");

        themeToggle = new ToggleButton("🌙 Dark");
        themeToggle.getStyleClass().add("theme-toggle");
        themeToggle.setOnAction(e -> toggleTheme());

        HBox titleBox = new HBox(20);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(title, themeToggle);
        HBox.setHgrow(title, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Search privately with MAArK...");
        searchField.setPrefHeight(40);
        searchField.getStyleClass().add("search-field");

        searchBtn = new Button("Search");
        searchBtn.setPrefHeight(40);
        searchBtn.getStyleClass().add("search-button");

        resultsList = new ListView<>();
        resultsList.getStyleClass().add("results-list");

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        statusLabel = new Label("Ready.");
        statusLabel.getStyleClass().add("status-label");

        SearchController controller = new SearchController(resultsList, statusLabel, webEngine);

        searchBtn.setOnAction(e -> controller.handleSearch(searchField.getText()));

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                controller.handleSearch(searchField.getText());
            }
        });

        resultsList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                String selectedResult = resultsList.getSelectionModel().getSelectedItem();
                if (selectedResult != null) {
                    String url = controller.extractUrl(selectedResult);
                    if (url != null && !url.isEmpty()) {
                        webEngine.load(url);
                    }
                }
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading page...");
            } else if (newState == Worker.State.SUCCEEDED) {
                statusLabel.setText("Page loaded.");
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load page.");
            }
        });

        HBox searchBox = new HBox(10, searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(resultsList, webView);
        splitPane.setDividerPositions(0.4);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        root = new VBox(15, titleBox, searchBox, splitPane, statusLabel);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("root");

        scene = new Scene(root, 1200, 700);
        applyLightTheme();
        
        stage.setTitle("MAArK - Privacy First Search Engine");
        stage.setScene(scene);
        stage.show();
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            applyDarkTheme();
            themeToggle.setText("☀️ Light");
        } else {
            applyLightTheme();
            themeToggle.setText("🌙 Dark");
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
