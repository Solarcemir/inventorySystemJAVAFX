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
        System.out.println("Mostrando Add product");
        // Lógica para mostrar la pantalla de agregar producto

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_product.fxml"));
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
    private void showInventory() {
        System.out.println("Mostrando Inventario");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/view_product.fxml"));
            Parent viewParent = loader.load();
            
            // Configurar para que ocupe todo el espacio del contentArea
            AnchorPane.setTopAnchor(viewParent, 0.0);
            AnchorPane.setBottomAnchor(viewParent, 0.0);
            AnchorPane.setLeftAnchor(viewParent, 0.0);
            AnchorPane.setRightAnchor(viewParent, 0.0);
            
            contentArea.getChildren().setAll(viewParent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showAnalytics() {
        System.out.println("Mostrando Analytics");
        // Lógica para mostrar la pantalla de analytics
    }
}