package com.maark.ui;

import com.maark.controller.SearchController;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MaarkApp extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("MAArK");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search privately with MAArK...");
        searchField.setPrefHeight(40);

        Button searchBtn = new Button("Search");
        searchBtn.setPrefHeight(40);

        ListView<String> resultsList = new ListView<>();

        Label statusLabel = new Label("Ready.");
        statusLabel.setStyle("-fx-text-fill: #666666;");

        SearchController controller = new SearchController(resultsList, statusLabel);

        // Event-driven programming: Button click
        searchBtn.setOnAction(e -> controller.handleSearch(searchField.getText()));

        // Event-driven programming: Enter key press
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                controller.handleSearch(searchField.getText());
            }
        });

        HBox searchBox = new HBox(10, searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox root = new VBox(15, title, searchBox, resultsList, statusLabel);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("MAArK - Privacy First Search Engine");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
