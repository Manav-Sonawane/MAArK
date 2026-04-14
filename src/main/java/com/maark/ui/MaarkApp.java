package com.maark.ui;

import com.maark.controller.SearchController;
import com.maark.privacy.AdBlockService;
import com.maark.privacy.BreachAlertService;
import com.maark.privacy.FingerprintAwarenessPanel;
import com.maark.privacy.PrivacyShieldManager;
import com.maark.util.SearchContext;
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
    private ListView<String> suggestionList;
    private ListView<com.maark.model.SearchResult> resultsList;
    private Label statusLabel;
    private ToggleButton themeToggle;
    private WebView webView;
    private VBox resultsPane;
    private SearchController controller;
    private ProgressBar loadingBar;

    // Privacy Services
    private PrivacyShieldManager privacyShield;
    private FingerprintAwarenessPanel fpPanel;
    private AdBlockService adBlockService;
    private BreachAlertService breachAlertService;

    @Override
    public void start(Stage stage) {
        // Enable hardware acceleration and performance optimizations
        System.setProperty("prism.order", "es2,sw");
        System.setProperty("prism.vsync", "false");
        System.setProperty("javafx.animation.fullspeed", "true");
        
        // Initialize privacy managers
        privacyShield = new PrivacyShieldManager();
        fpPanel = new FingerprintAwarenessPanel(privacyShield);
        adBlockService = new AdBlockService(true);
        breachAlertService = new BreachAlertService(message -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
            alert.setHeaderText("Breach Alert Detected!");
            alert.showAndWait();
        });

        Button backBtn = new Button("◀");
        Button forwardBtn = new Button("▶");
        Button reloadBtn = new Button("↻");
        Button homeBtn = new Button("🏠");
        
        ToggleButton shieldToggle = new ToggleButton("🛡️ Shield OFF");
        shieldToggle.getStyleClass().add("shield-toggle");
        shieldToggle.setOnAction(e -> {
            boolean active = privacyShield.toggleShield();
            shieldToggle.setText(active ? "🛡️ Shield ON" : "🛡️ Shield OFF");
            if (active) {
                shieldToggle.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                breachAlertService.clearSessionCache(); // clear cache on new ephemeral session
            } else {
                shieldToggle.setStyle("");
            }
            // Update global HTTP Client for Proxy
            SearchContext.setClient(privacyShield.buildPrivacyHttpClient());
            // Update WebEngine UA
            if (webView != null) {
                webView.getEngine().setUserAgent(privacyShield.getActiveUserAgent());
            }
        });

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

        suggestionList = new ListView<>();
        suggestionList.setMaxHeight(150);

        searchBtn = new Button("🔍");
        searchBtn.setPrefHeight(35);
        searchBtn.getStyleClass().add("search-button");

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10));
        toolbar.getStyleClass().add("toolbar");
        toolbar.getChildren().addAll(backBtn, forwardBtn, reloadBtn, homeBtn, searchField, searchBtn, shieldToggle, themeToggle);

        // Add loading progress bar
        loadingBar = new ProgressBar();
        loadingBar.setPrefHeight(3);
        loadingBar.setMaxWidth(Double.MAX_VALUE);
        loadingBar.setVisible(false);
        loadingBar.getStyleClass().add("loading-bar");

        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(toolbar, loadingBar);

        resultsList = new ListView<>();
        resultsList.getStyleClass().add("results-list");

        resultsPane = new VBox(resultsList);
        resultsPane.setVisible(false);
        resultsPane.setManaged(false);
        resultsPane.getStyleClass().add("results-pane");
        VBox.setVgrow(resultsList, Priority.ALWAYS);

        webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        
        // Enable WebView performance optimizations
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserAgent(privacyShield.getActiveUserAgent());
        
        // Enable caching (improves repeat page loads)
        try {
            java.lang.reflect.Method setCache = webEngine.getClass().getDeclaredMethod("setDataDirectoryCreated", Boolean.TYPE);
            setCache.setAccessible(true);
            setCache.invoke(webEngine, true);
        } catch (Exception ex) {
            // Cache setting failed, continue anyway
        }

        statusLabel = new Label("Ready.");
        statusLabel.getStyleClass().add("status-label");

        BorderPane bottomBar = new BorderPane();
        bottomBar.setLeft(statusLabel);
        bottomBar.setPadding(new Insets(2, 5, 2, 5));
        bottomBar.getStyleClass().add("bottom-bar");

        controller = new SearchController(searchField, suggestionList, resultsList, statusLabel, webEngine) {
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
                com.maark.model.SearchResult selectedResult = resultsList.getSelectionModel().getSelectedItem();
                if (selectedResult != null) {
                    controller.handleResultClick(selectedResult);
                    hideResults();
                }
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading: " + webEngine.getLocation());
                loadingBar.setVisible(true);
                if (!webEngine.getLocation().isEmpty() && !webEngine.getLocation().startsWith("data:text/html")) {
                    searchField.setText(webEngine.getLocation());
                    breachAlertService.inspectUrl(webEngine.getLocation());
                }
            } else if (newState == Worker.State.SUCCEEDED) {
                statusLabel.setText("Done.");
                loadingBar.setVisible(false);
                // Inject AdBlock / Ephemeral session cleanup
                adBlockService.injectIntoPage(webEngine, privacyShield.isEphemeralEnabled());
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load: " + webEngine.getLocation());
                loadingBar.setVisible(false);
            }
        });
        
        // Bind loading progress
        loadingBar.progressProperty().bind(webEngine.getLoadWorker().progressProperty());

        StackPane centerPane = new StackPane();
        centerPane.getChildren().addAll(webView, resultsPane);
        StackPane.setAlignment(resultsPane, Pos.TOP_CENTER);
        resultsPane.setMaxWidth(800);
        resultsPane.setMaxHeight(500);
        StackPane.setMargin(resultsPane, new Insets(0, 0, 0, 0));

        root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(centerPane);
        root.setRight(fpPanel);
        root.setBottom(bottomBar);

        scene = new Scene(root, 1200, 700);
        applyLightTheme();

        homeBtn.fire();

        stage.setTitle("MAArK Browser");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            fpPanel.shutdown();
            breachAlertService.shutdown();
        });
        stage.show();
    }

    private void showResults() {
        resultsPane.setVisible(true);
        resultsPane.setManaged(true);
        // Hide panel when showing results
        fpPanel.setVisible(false);
        fpPanel.setManaged(false);
    }

    private void hideResults() {
        resultsPane.setVisible(false);
        resultsPane.setManaged(false);
        // Show panel when browsing
        fpPanel.setVisible(true);
        fpPanel.setManaged(true);
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
