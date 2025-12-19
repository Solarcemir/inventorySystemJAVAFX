package com.inventory.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        // Inicialización si es necesaria
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenido al Sistema de Inventario");
        }
    }
}