package com.inventory.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
// using path lib instead of local
import java.nio.file.StandardOpenOption;

public class HomeController {

    @FXML
    private FlowPane notesContainer;

    @FXML
    private Button newNoteButton;

    @FXML
    private Label main_product_count;
    @FXML
    private Label main_clients_count;
    @FXML
    private Label main_sales_count;

    private final Path NOTES_FILE = Path.of("notes.txt");

    @FXML
    public void initialize() {
        loadNotes();
        loadCounts();
    }

    private void loadCounts() {
        // Productos
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/products/count");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    long count = new java.util.Scanner(conn.getInputStream()).nextLong();
                    Platform.runLater(() -> main_product_count.setText(String.valueOf(count)));
                }
            } catch (Exception e) { Platform.runLater(() -> main_product_count.setText("--")); }
        }).start();
        // Clientes
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/clients/count");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    long count = new java.util.Scanner(conn.getInputStream()).nextLong();
                    Platform.runLater(() -> main_clients_count.setText(String.valueOf(count)));
                }
            } catch (Exception e) { Platform.runLater(() -> main_clients_count.setText("--")); }
        }).start();
        // Ventas
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/sales/count");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    long count = new java.util.Scanner(conn.getInputStream()).nextLong();
                    Platform.runLater(() -> main_sales_count.setText(String.valueOf(count)));
                }
            } catch (Exception e) { Platform.runLater(() -> main_sales_count.setText("--")); }
        }).start();
    }

    /**
     * Cargar notas desde el archivo al iniciar
     */
    private void loadNotes() {
        if (!Files.exists(NOTES_FILE)) {
            return;
        }

        try {
            String content = Files.readString(NOTES_FILE);
            String[] notes = content.split("---NOTE---");

            for (String note : notes) {
                if (!note.trim().isEmpty()) {
                    createNote(note.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Crear una nota visual (card)
     */
    private void createNote(String text) {
        TextArea noteArea = new TextArea(text);
        noteArea.setWrapText(true);
        noteArea.setPrefHeight(100);
        noteArea.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #333333;
            -fx-font-size: 13px;
            -fx-control-inner-background: transparent;
        """);

        // Botón de eliminar
        Button deleteButton = new Button("✕");
        deleteButton.setStyle("""
            -fx-background-color: #FF6B6B;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-cursor: hand;
            -fx-background-radius: 50;
            -fx-min-width: 25;
            -fx-min-height: 25;
            -fx-max-width: 25;
            -fx-max-height: 25;
            -fx-padding: 0;
        """);

        // Header con botón de eliminar
        HBox header = new HBox(deleteButton);
        header.setAlignment(Pos.TOP_RIGHT);
        header.setStyle("-fx-padding: 5 5 0 0;");

        VBox card = new VBox(5, header, noteArea);
        card.setPrefWidth(270);
        card.setStyle("""
            -fx-background-color: #FFFACD;
            -fx-padding: 10;
            -fx-background-radius: 12;
            -fx-border-color: #FFD700;
            -fx-border-width: 1;
            -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);
        """);

        // Eliminar nota
        deleteButton.setOnAction(e -> {
            notesContainer.getChildren().remove(card);
            saveAllNotes();
        });

        // Auto-save al perder foco
        noteArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveAllNotes();
            }
        });

        // Auto-save mientras escribe
        noteArea.textProperty().addListener((obs, oldText, newText) -> {
            saveAllNotes();
        });

        notesContainer.getChildren().add(card);
    }

    /**
     * Guardar todas las notas automáticamente
     */
    private void saveAllNotes() {
        StringBuilder sb = new StringBuilder();

        for (Node node : notesContainer.getChildren()) {
            VBox card = (VBox) node;
            TextArea area = (TextArea) card.getChildren().get(1);

            sb.append("---NOTE---\n");
            sb.append(area.getText()).append("\n");
        }

        try {
            Files.writeString(
                NOTES_FILE,
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Crear nueva nota vacía
     */
    @FXML
    public void newNote() {
        createNote("");
    }
}
