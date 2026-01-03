package com.inventory.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import javafx.stage.Popup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class AddProductController {
    @FXML private TextField product_name;
    @FXML private TextField vendor;
    @FXML private TextField category;
    @FXML private TextField description;
    @FXML private TextField cost_price;
    @FXML private TextField product_price;
    @FXML private Spinner<Integer> quantity_spinner;
    @FXML private TextField img_url;
    @FXML private Button button_browse;
    @FXML private Button submitButton;

    // Preview panel
    @FXML private ImageView load_image;
    @FXML private Label product_name_map;
    @FXML private Label vendor_map;
    @FXML private Label category_map;
    @FXML private Label cost_map;
    @FXML private Label price_map;
    @FXML private Label profit_map;
    @FXML private Label quantity_map;
    @FXML private Label description_map;

    @FXML
    public void initialize() {
        quantity_spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));
        button_browse.setOnAction(e -> browseImage());
        submitButton.setOnAction(e -> saveProduct());
    }

    private void browseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(button_browse.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Copiar imagen a carpeta local del programa
                String copiedPath = copyImageToLocalFolder(selectedFile);
                img_url.setText(copiedPath);
                
                // Mostrar preview
                Image img = new Image(new File(copiedPath).toURI().toString());
                load_image.setImage(img);
            } catch (Exception e) {
                showAlert("Error copying image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private String copyImageToLocalFolder(File sourceFile) throws Exception {
        // Crear carpeta de imágenes si no existe
        Path imagesDir = Paths.get("product-images");
        if (!Files.exists(imagesDir)) {
            Files.createDirectories(imagesDir);
        }
        
        // Generar nombre único para la imagen
        String extension = "";
        String fileName = sourceFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex);
        }
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        
        // Copiar archivo
        Path targetPath = imagesDir.resolve(uniqueFileName);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        return targetPath.toString();
    }

    private void saveProduct() {
        String name = product_name.getText();
        String provider = vendor.getText();
        String cat = category.getText();
        String desc = description.getText();
        String costText = cost_price.getText();
        String priceText = product_price.getText();
        int quantity = quantity_spinner.getValue();
        String imagePath = img_url.getText();

        if (name.isEmpty() || priceText.isEmpty() || costText.isEmpty()) {
            showAlert("Missing required fields (Name, Cost Price, Sale Price)");
            return;
        }
        if (quantity < 1) {
            showAlert("Quantity must be at least 1");
            return;
        }
        BigDecimal costPrice;
        BigDecimal price;
        try {
            costPrice = new BigDecimal(costText);
            price = new BigDecimal(priceText);
        } catch (NumberFormatException e) {
            showAlert("Invalid price format");
            return;
        }
        
        // Enviar al backend
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/products");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                // Escapar comillas y caracteres especiales
                String escapedName = name.replace("\"", "\\\"").replace("\\", "\\\\");
                String escapedProvider = provider.replace("\"", "\\\"").replace("\\", "\\\\");
                String escapedCat = cat.replace("\"", "\\\"").replace("\\", "\\\\");
                String escapedDesc = desc.replace("\"", "\\\"").replace("\\", "\\\\");
                String escapedImagePath = imagePath.replace("\\", "/");
                
                String json = String.format(
                    "{\"productName\":\"%s\",\"provider\":\"%s\",\"category\":\"%s\",\"description\":\"%s\",\"costPrice\":%s,\"price\":%s,\"productQuantity\":%d,\"imagePath\":\"%s\"}",
                    escapedName, escapedProvider, escapedCat, escapedDesc, costPrice.toString(), price.toString(), quantity, escapedImagePath
                );
                
                System.out.println("Sending JSON: " + json);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes("UTF-8"));
                }

                int responseCode = conn.getResponseCode();
                System.out.println("Response Code: " + responseCode);
                
                // Leer respuesta del servidor para debug
                if (responseCode >= 400) {
                    java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream())
                    );
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Error response: " + response.toString());
                }
                
                Platform.runLater(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        System.out.println("Product saved successfully! Showing popup...");
                        updatePreview(name, provider, cat, desc, costPrice, price, quantity, imagePath);
                        showSuccessPopup();
                        clearForm();
                    } else {
                        showAlert("Error saving product. Response code: " + responseCode);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Cannot connect to server"));
                e.printStackTrace();
            }
        }).start();
    }
    
    private void clearForm() {
        product_name.clear();
        vendor.clear();
        category.clear();
        description.clear();
        cost_price.clear();
        product_price.clear();
        img_url.clear();
        quantity_spinner.getValueFactory().setValue(0);
    }
    
    private void showSuccessPopup() {
        System.out.println("showSuccessPopup() called");
        
        // Usar Alert temporalmente para confirmar que funciona
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("✓ Product saved successfully!");
        alert.show();
        
        // Auto-cerrar después de 2 segundos
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> alert.close());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updatePreview(String name, String provider, String cat, String desc, BigDecimal costPrice, BigDecimal price, int quantity, String imagePath) {
        product_name_map.setText(name);
        vendor_map.setText("Vendor: " + provider);
        category_map.setText("Category: " + cat);
        cost_map.setText("Cost: $" + costPrice);
        price_map.setText("Sale: $" + price);
        BigDecimal profit = price.subtract(costPrice);
        profit_map.setText("Profit: $" + profit + " (" + (costPrice.compareTo(BigDecimal.ZERO) > 0 ? profit.multiply(new BigDecimal(100)).divide(costPrice, 1, java.math.RoundingMode.HALF_UP) : "0") + "%)");
        quantity_map.setText("Quantity: " + quantity);
        description_map.setText("Description: " + desc);
        if (imagePath != null && !imagePath.isEmpty()) {
            Image img = new Image(new File(imagePath).toURI().toString());
            load_image.setImage(img);
        } else {
            load_image.setImage(null);
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
