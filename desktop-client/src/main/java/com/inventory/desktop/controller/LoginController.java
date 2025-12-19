package com.inventory.desktop.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;


public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;

    @FXML
     private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonInput = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonInput.getBytes());
                }

                int responseCode = conn.getResponseCode();
                boolean loginSuccess = false;
                if (responseCode == 200) {
                    // El backend devuelve true o false como texto plano
                    loginSuccess = new java.util.Scanner(conn.getInputStream()).nextBoolean();
                }

                boolean finalLoginSuccess = loginSuccess;
                Platform.runLater(() -> {
                    Alert alert = new Alert(finalLoginSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                    alert.setTitle("Login");
                    alert.setHeaderText(null);
                    alert.setContentText(finalLoginSuccess ? "Login exitoso" : "Usuario o contraseña incorrectos");
                    alert.showAndWait();



                    if (finalLoginSuccess) {
                    // Cargar nueva escena de productos
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root);
                        Stage stage = (Stage) usernameField.getScene().getWindow();
                        stage.setScene(scene);
                        stage.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }



                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("No se pudo conectar al servidor");
                    alert.showAndWait();
                });
            }



         



        }).start();
    }
}
