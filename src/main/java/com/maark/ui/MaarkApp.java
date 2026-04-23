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
import java.util.List;

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
    private TabPane tabPane;
    private VBox resultsPane;
    private SearchController controller;
    private ProgressBar loadingBar;
    private com.maark.manager.HistoryManager historyManager;

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
        Button addTabBtn = new Button("+");
        Button historyBtn = new Button("📜 History");
        
        historyManager = new com.maark.manager.HistoryManager();
        
        ToggleButton shieldToggle = new ToggleButton("🛡️ Shield OFF");
        shieldToggle.setSelected(false); // Always starts OFF so homepage loads cleanly
        shieldToggle.getStyleClass().add("shield-toggle");
        shieldToggle.setOnAction(e -> {
            boolean active = privacyShield.toggleShield();
            shieldToggle.setText(active ? "🛡️ Shield ON" : "🛡️ Shield OFF");
            if (active) {
                shieldToggle.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                breachAlertService.clearSessionCache();
            } else {
                shieldToggle.setStyle("");
            }
            // Update global HTTP Client for Proxy
            SearchContext.setClient(privacyShield.buildPrivacyHttpClient());
            // Update WebEngine UA + reload current page so changes take effect now
            WebEngine activeEngine = getActiveEngine();
            if (activeEngine != null) {
                activeEngine.setUserAgent(privacyShield.getActiveUserAgent());
                String currentUrl = activeEngine.getLocation();
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    activeEngine.reload(); // Apply new proxy/UA immediately
                }
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
        toolbar.getChildren().addAll(backBtn, forwardBtn, reloadBtn, homeBtn, addTabBtn, searchField, searchBtn, historyBtn, shieldToggle, themeToggle);

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

        tabPane = new TabPane();
        tabPane.getStyleClass().add("browser-tab-pane");
        HBox.setHgrow(tabPane, Priority.ALWAYS);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Selection listener to sync UI with active tab
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                WebView view = (WebView) newTab.getContent();
                WebEngine engine = view.getEngine();
                controller.setWebEngine(engine);
                searchField.setText(engine.getLocation());
                loadingBar.progressProperty().unbind();
                loadingBar.progressProperty().bind(engine.getLoadWorker().progressProperty());
                loadingBar.setVisible(engine.getLoadWorker().getState() == Worker.State.RUNNING);
            }
        });
        


        statusLabel = new Label("Ready.");
        statusLabel.getStyleClass().add("status-label");

        BorderPane bottomBar = new BorderPane();
        bottomBar.setLeft(statusLabel);
        bottomBar.setPadding(new Insets(2, 5, 2, 5));
        bottomBar.getStyleClass().add("bottom-bar");

        // Temporary dummy engine for controller initialization
        controller = new SearchController(searchField, suggestionList, resultsList, statusLabel, null) {
            @Override
            public void handleSearch(String query) {
                if (query.isEmpty()) return;
                WebEngine engine = getActiveEngine();
                if (engine == null) return;

                if (query.startsWith("http://") || query.startsWith("https://")) {
                    engine.load(query);
                    hideResults();
                } else if (query.contains(".") && !query.contains(" ")) {
                    engine.load("https://" + query);
                    hideResults();
                } else {
                    try {
                        String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8.toString());
                        engine.load("https://duckduckgo.com/?q=" + encoded);
                    } catch (Exception e) {
                        engine.load("https://duckduckgo.com");
                    }
                    super.handleSearch(query);
                    hideResults(); 
                }
            }
        };

        createNewTab("https://duckduckgo.com");

        searchBtn.setOnAction(e -> controller.handleSearch(searchField.getText()));

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                controller.handleSearch(searchField.getText());
            }
        });

        backBtn.setOnAction(e -> {
            WebEngine engine = getActiveEngine();
            if (engine != null && engine.getHistory().getCurrentIndex() > 0) {
                engine.getHistory().go(-1);
                hideResults();
            }
        });
        forwardBtn.setOnAction(e -> {
            WebEngine engine = getActiveEngine();
            if (engine != null && engine.getHistory().getCurrentIndex() < engine.getHistory().getEntries().size() - 1) {
                engine.getHistory().go(1);
                hideResults();
            }
        });
        reloadBtn.setOnAction(e -> {
            WebEngine engine = getActiveEngine();
            if (engine != null) {
                engine.reload();
                hideResults();
            }
        });
        homeBtn.setOnAction(e -> {
            WebEngine engine = getActiveEngine();
            if (engine != null) {
                engine.load("https://duckduckgo.com");
                searchField.clear();
                hideResults();
            }
        });
        addTabBtn.setOnAction(e -> createNewTab("https://duckduckgo.com"));
        historyBtn.setOnAction(e -> showHistory());

        resultsList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                com.maark.model.SearchResult selectedResult = resultsList.getSelectionModel().getSelectedItem();
                if (selectedResult != null) {
                    controller.handleResultClick(selectedResult);
                    hideResults();
                }
            }
        });


        StackPane centerPane = new StackPane();
        centerPane.getChildren().addAll(tabPane, resultsPane);
        StackPane.setAlignment(resultsPane, Pos.TOP_CENTER);
        resultsPane.setMaxWidth(800);
        resultsPane.setMaxHeight(500);
        StackPane.setMargin(resultsPane, new Insets(50, 0, 0, 0)); // Lower to accommodate tabs

        root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(centerPane);
        root.setRight(fpPanel);
        root.setBottom(bottomBar);

        scene = new Scene(root, 1200, 700);
        applyLightTheme();

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
        fpPanel.setVisible(true);
        fpPanel.setManaged(true);
    }

    private WebEngine getActiveEngine() {
        Tab activeTab = tabPane.getSelectionModel().getSelectedItem();
        if (activeTab != null) {
            return ((WebView) activeTab.getContent()).getEngine();
        }
        return null;
    }

    private void createNewTab(String url) {
        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent(privacyShield.getActiveUserAgent());
        
        try {
            java.lang.reflect.Method setCache = engine.getClass().getDeclaredMethod("setDataDirectoryCreated", Boolean.TYPE);
            setCache.setAccessible(true);
            setCache.invoke(engine, true);
        } catch (Exception ex) {}

        Tab tab = new Tab("New Tab", view);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                statusLabel.setText("Loading: " + engine.getLocation());
                loadingBar.setVisible(true);
                if (!engine.getLocation().isEmpty() && !engine.getLocation().startsWith("data:text/html")) {
                    searchField.setText(engine.getLocation());
                    breachAlertService.inspectUrl(engine.getLocation());
                }
            } else if (newState == Worker.State.SUCCEEDED) {
                statusLabel.setText("Done.");
                loadingBar.setVisible(false);
                adBlockService.injectIntoPage(engine, privacyShield.isEphemeralEnabled());
                
                String title = engine.getTitle();
                if (title == null || title.isEmpty()) title = engine.getLocation();
                tab.setText(title.length() > 20 ? title.substring(0, 17) + "..." : title);
                
                // Add to local history
                if (!engine.getLocation().startsWith("data:text/html")) {
                    historyManager.addBrowse(engine.getLocation(), title);
                }
            } else if (newState == Worker.State.FAILED) {
                statusLabel.setText("Failed to load: " + engine.getLocation());
                loadingBar.setVisible(false);
            }
        });

        engine.load(url);
    }

    private void showHistory() {
        resultsList.getItems().clear();
        
        // Add searches
        List<com.maark.model.SearchHistoryEntry> searches = historyManager.getRecentSearches(50);
        for (com.maark.model.SearchHistoryEntry s : searches) {
            resultsList.getItems().add(new com.maark.model.SearchResult(
                "🔍 " + s.getQuery(), 
                "Search History", 
                "https://duckduckgo.com/?q=" + s.getQuery(),
                "Local History"
            ));
        }
        
        // Add browses
        List<com.maark.model.BrowseHistoryEntry> browses = historyManager.getRecentBrowses(50);
        for (com.maark.model.BrowseHistoryEntry b : browses) {
            resultsList.getItems().add(new com.maark.model.SearchResult(
                "🌐 " + b.getTitle(), 
                b.getUrl(), 
                b.getUrl(),
                "Local History"
            ));
        }
        
        if (resultsList.getItems().isEmpty()) {
            resultsList.getItems().add(new com.maark.model.SearchResult("No history found.", "", "", ""));
        }
        
        showResults();
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
