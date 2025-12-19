package com.inventory.desktop.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class MainController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private AnchorPane contentArea;

    @FXML
    public void initialize() {
        // Inicialización si es necesaria
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenido al Sistema de Inventario");
        }
    }

    @FXML
    private void showHome() {
        System.out.println("Mostrando Home");
        // Lógica para mostrar la pantalla de inicio

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Parent homeViewParent = loader.load();
            
            // Configurar para que ocupe todo el espacio del contentArea
            AnchorPane.setTopAnchor(homeViewParent, 0.0);
            AnchorPane.setBottomAnchor(homeViewParent, 0.0);
            AnchorPane.setLeftAnchor(homeViewParent, 0.0);
            AnchorPane.setRightAnchor(homeViewParent, 0.0);
            
            contentArea.getChildren().setAll(homeViewParent);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void showAddProduct() {
        System.out.println("Mostrando Agregar Producto");
        // Lógica para mostrar la pantalla de agregar producto
    }

    @FXML
    private void showInventory() {
        System.out.println("Mostrando Inventario");
        // Lógica para mostrar la pantalla de inventario
    }

    @FXML
    private void showAnalytics() {
        System.out.println("Mostrando Analytics");
        // Lógica para mostrar la pantalla de analytics
    }
}