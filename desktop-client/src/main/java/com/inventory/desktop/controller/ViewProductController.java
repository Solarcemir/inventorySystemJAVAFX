package com.inventory.desktop.controller;

import com.inventory.desktop.model.Client;
import com.inventory.desktop.model.Product;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ViewProductController {

    @FXML private TableView<Product> table_view;
    @FXML private TableColumn<Product, Long> col_id;
    @FXML private TableColumn<Product, String> col_name;
    @FXML private TableColumn<Product, String> col_provider;
    @FXML private TableColumn<Product, String> col_category;
    @FXML private TableColumn<Product, Double> col_price;
    @FXML private TableColumn<Product, Integer> col_quantity;
    
    @FXML private ImageView table_image;
    @FXML private TextField search_field;
    @FXML private Button sell_button;
    @FXML private Button edit_button;
    @FXML private Button delete_button;
    
    @FXML private Label detail_name;
    @FXML private Label detail_provider;
    @FXML private Label detail_category;
    @FXML private Label detail_description;
    @FXML private Label detail_price;
    @FXML private Label detail_quantity;

    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredData;
    private Product selectedProduct = null;

    @FXML
    public void initialize() {
        // Configurar columnas del TableView
        col_id.setCellValueFactory(new PropertyValueFactory<>("productId"));
        col_name.setCellValueFactory(new PropertyValueFactory<>("productName"));
        col_provider.setCellValueFactory(new PropertyValueFactory<>("provider"));
        col_category.setCellValueFactory(new PropertyValueFactory<>("category"));
        col_price.setCellValueFactory(new PropertyValueFactory<>("price"));
        col_quantity.setCellValueFactory(new PropertyValueFactory<>("productQuantity"));

        // Configurar lista filtrable
        filteredData = new FilteredList<>(productList, p -> true);
        table_view.setItems(filteredData);

        // Listener para selección en la tabla
        table_view.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedProduct = newSelection;
                displayProductDetails(newSelection);
            }
        });

        // Configurar búsqueda
        search_field.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });

        // Configurar botones
        sell_button.setOnAction(e -> sellSelectedProduct());
        delete_button.setOnAction(e -> deleteSelectedProduct());
        edit_button.setOnAction(e -> editSelectedProduct());
        
        // Click en imagen para ver más grande
        table_image.setOnMouseClicked(e -> showImagePopup());

        // Cargar productos desde el backend
        loadProducts();
    }

    private void loadProducts() {
        System.out.println("Loading products from backend...");
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/products");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                System.out.println("Response code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    
                    System.out.println("Response: " + response.toString());

                    // Parsear JSON
                    JSONArray jsonArray = new JSONArray(response.toString());
                    System.out.println("Found " + jsonArray.length() + " products");
                    
                    Platform.runLater(() -> {
                        productList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                Product product = new Product(
                                    obj.optLong("id", 0),
                                    obj.optString("productName", "Unknown"),
                                    obj.optString("provider", ""),
                                    obj.optString("category", ""),
                                    obj.optString("description", ""),
                                    obj.optDouble("costPrice", 0.0),
                                    obj.optDouble("price", 0.0),
                                    obj.optInt("productQuantity", 0),
                                    obj.optString("imagePath", "")
                                );
                                productList.add(product);
                            } catch (Exception ex) {
                                System.out.println("Error parsing product: " + ex.getMessage());
                            }
                        }
                        System.out.println("Product list size: " + productList.size());
                    });
                } else {
                    System.out.println("Error response code: " + responseCode);
                    Platform.runLater(() -> showAlert("Error loading products. Response code: " + responseCode));
                }
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                Platform.runLater(() -> showAlert("Cannot connect to server: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void displayProductDetails(Product product) {
        detail_name.setText(product.getProductName());
        detail_provider.setText("Provider: " + product.getProvider());
        detail_category.setText("Category: " + product.getCategory());
        detail_description.setText("Description: " + product.getDescription());
        detail_price.setText("Cost: $" + String.format("%.2f", product.getCostPrice()) + " | Sale: $" + String.format("%.2f", product.getPrice()) + " | Profit: $" + String.format("%.2f", product.getProfit()));
        detail_quantity.setText("Quantity: " + product.getProductQuantity());

        // Cargar imagen
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            try {
                File imageFile = new File(product.getImagePath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    table_image.setImage(image);
                } else {
                    table_image.setImage(null);
                }
            } catch (Exception e) {
                table_image.setImage(null);
                e.printStackTrace();
            }
        } else {
            table_image.setImage(null);
        }
    }

    private void filterProducts(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredData.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredData.setPredicate(product -> {
                return product.getProductName().toLowerCase().contains(lowerCaseFilter) ||
                       product.getProvider().toLowerCase().contains(lowerCaseFilter) ||
                       product.getCategory().toLowerCase().contains(lowerCaseFilter);
            });
        }
    }

    private void sellSelectedProduct() {
        if (selectedProduct == null) {
            showAlert("Please select a product to sell");
            return;
        }

        if (selectedProduct.getProductQuantity() < 1) {
            showAlert("No stock available for this product");
            return;
        }

        // Crear diálogo personalizado para venta con opción de cliente
        showSellDialog(selectedProduct);
    }
    
    /**
     * Muestra un diálogo personalizado para vender un producto.
     * Permite seleccionar opcionalmente un cliente para atribuirle la venta.
     */
    private void showSellDialog(Product product) {
        // Crear el diálogo
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sell Product");
        dialog.setHeaderText("Sell: " + product.getProductName());
        
        // Botones
        ButtonType sellButtonType = new ButtonType("Sell", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sellButtonType, ButtonType.CANCEL);
        
        // Contenido del diálogo
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));
        
        // Campo de cantidad
        Spinner<Integer> quantitySpinner = new Spinner<>(1, product.getProductQuantity(), 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(100);
        
        // ComboBox de clientes (opcional)
        ComboBox<Client> clientComboBox = new ComboBox<>();
        clientComboBox.setPromptText("-- No client (optional) --");
        clientComboBox.setPrefWidth(200);
        
        // Configurar cómo mostrar los clientes en el ComboBox
        clientComboBox.setConverter(new StringConverter<Client>() {
            @Override
            public String toString(Client client) {
                if (client == null) return null;
                return client.getFirstName() + " " + client.getLastName() + " (" + client.getEmail() + ")";
            }
            
            @Override
            public Client fromString(String string) {
                return null; // No necesario para ComboBox no editable
            }
        });
        
        // Botón para limpiar selección de cliente
        Button clearClientBtn = new Button("✕");
        clearClientBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6;");
        clearClientBtn.setOnAction(e -> clientComboBox.getSelectionModel().clearSelection());
        
        HBox clientBox = new HBox(5, clientComboBox, clearClientBtn);
        clientBox.setAlignment(Pos.CENTER_LEFT);
        
        // Label para mostrar precio total
        Label totalLabel = new Label("Total: $" + String.format("%.2f", product.getPrice()));
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        
        // Actualizar total cuando cambia la cantidad
        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            double total = product.getPrice() * newVal;
            totalLabel.setText("Total: $" + String.format("%.2f", total));
        });
        
        // Añadir al grid
        grid.add(new Label("Available Stock:"), 0, 0);
        grid.add(new Label(String.valueOf(product.getProductQuantity())), 1, 0);
        grid.add(new Label("Quantity to sell:"), 0, 1);
        grid.add(quantitySpinner, 1, 1);
        grid.add(new Label("Assign to client:"), 0, 2);
        grid.add(clientBox, 1, 2);
        grid.add(totalLabel, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Cargar clientes en el ComboBox
        loadClientsForSale(clientComboBox);
        
        // Mostrar diálogo y procesar resultado
        dialog.showAndWait().ifPresent(result -> {
            if (result == sellButtonType) {
                int quantity = quantitySpinner.getValue();
                Client selectedClient = clientComboBox.getSelectionModel().getSelectedItem();
                
                if (selectedClient != null) {
                    // Venta CON cliente
                    createSaleWithClient(product.getProductId(), selectedClient.getId(), quantity);
                } else {
                    // Venta SIN cliente
                    createSale(product.getProductId(), quantity);
                }
            }
        });
    }
    
    /**
     * Carga la lista de clientes desde el backend para el ComboBox de ventas
     */
    private void loadClientsForSale(ComboBox<Client> comboBox) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/clients");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    
                    JSONArray jsonArray = new JSONArray(response.toString());
                    ObservableList<Client> clients = FXCollections.observableArrayList();
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        Client client = new Client(
                            obj.optLong("id", 0),
                            obj.optString("firstName", ""),
                            obj.optString("lastName", ""),
                            obj.optString("email", ""),
                            obj.optString("phoneNumber", ""),
                            obj.optDouble("spentAmount", 0.0)
                        );
                        clients.add(client);
                    }
                    
                    Platform.runLater(() -> comboBox.setItems(clients));
                }
            } catch (Exception e) {
                System.out.println("Error loading clients for sale: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Crea una venta CON cliente asociado (actualiza spentAmount del cliente)
     */
    private void createSaleWithClient(long productId, long clientId, int quantity) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/sales/with-client?productId=" + productId 
                    + "&clientId=" + clientId + "&quantity=" + quantity);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200 || responseCode == 201) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    
                    JSONObject saleObj = new JSONObject(response.toString());
                    double totalAmount = saleObj.optDouble("totalAmount", 0.0);
                    
                    // Obtener nombre del cliente de la respuesta
                    String clientName = "";
                    if (saleObj.has("client") && !saleObj.isNull("client")) {
                        JSONObject clientObj = saleObj.getJSONObject("client");
                        clientName = clientObj.optString("firstName", "") + " " + clientObj.optString("lastName", "");
                    }
                    final String finalClientName = clientName;
                    
                    Platform.runLater(() -> {
                        showInfo("Sale completed!\n" +
                                "Quantity: " + quantity + "\n" +
                                "Total: $" + String.format("%.2f", totalAmount) + "\n" +
                                "Client: " + finalClientName + "\n" +
                                "(Client's spent amount has been updated)");
                        loadProducts();
                    });
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    Platform.runLater(() -> showAlert("Error creating sale: " + errorResponse.toString()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Cannot connect to server: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void createSale(long productId, int quantity) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/sales?productId=" + productId + "&quantity=" + quantity);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200 || responseCode == 201) {
                    // Leer respuesta para mostrar el total
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    
                    JSONObject saleObj = new JSONObject(response.toString());
                    double totalAmount = saleObj.optDouble("totalAmount", 0.0);
                    
                    Platform.runLater(() -> {
                        showInfo("Sale completed!\nQuantity: " + quantity + "\nTotal: $" + String.format("%.2f", totalAmount));
                        loadProducts(); // Recargar para actualizar stock
                    });
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    Platform.runLater(() -> showAlert("Error creating sale: " + errorResponse.toString()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Cannot connect to server: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteSelectedProduct() {
        if (selectedProduct == null) {
            showAlert("Please select a product to delete");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Product");
        confirmation.setHeaderText("Delete " + selectedProduct.getProductName() + "?");
        confirmation.setContentText("The product will be removed from inventory.\nSales history will be preserved for analytics.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteProductFromBackend(selectedProduct.getProductId());
            }
        });
    }

    private void deleteProductFromBackend(long productId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/products/" + productId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");

                int responseCode = conn.getResponseCode();
                Platform.runLater(() -> {
                    if (responseCode == 200 || responseCode == 204) {
                        showInfo("Product deleted successfully");
                        loadProducts(); // Recargar lista
                    } else {
                        showAlert("Error deleting product. Response code: " + responseCode);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Cannot connect to server: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void editSelectedProduct() {
        if (selectedProduct == null) {
            showAlert("Please select a product to edit");
            return;
        }
        
        showEditProductDialog(selectedProduct);
    }

    /**
     * Muestra un diálogo para editar el producto seleccionado
     */
    private void showEditProductDialog(Product product) {
        // Crear el diálogo
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Edit Product");
        dialog.setHeaderText("Edit: " + product.getProductName());

        // Configurar botones
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Crear el formulario
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campos del formulario pre-llenados con datos actuales
        TextField nameField = new TextField(product.getProductName());
        nameField.setPromptText("Product Name");
        
        TextField providerField = new TextField(product.getProvider());
        providerField.setPromptText("Provider");
        
        TextField categoryField = new TextField(product.getCategory());
        categoryField.setPromptText("Category");
        
        TextField descriptionField = new TextField(product.getDescription());
        descriptionField.setPromptText("Description");
        
        TextField costPriceField = new TextField(String.valueOf(product.getCostPrice()));
        costPriceField.setPromptText("Cost Price");
        
        TextField salePriceField = new TextField(String.valueOf(product.getPrice()));
        salePriceField.setPromptText("Sale Price");
        
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 100000, product.getProductQuantity());
        quantitySpinner.setEditable(true);

        // Agregar campos al grid
        grid.add(new Label("Product Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Provider:"), 0, 1);
        grid.add(providerField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descriptionField, 1, 3);
        grid.add(new Label("Cost Price:"), 0, 4);
        grid.add(costPriceField, 1, 4);
        grid.add(new Label("Sale Price:"), 0, 5);
        grid.add(salePriceField, 1, 5);
        grid.add(new Label("Quantity:"), 0, 6);
        grid.add(quantitySpinner, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Enfocar el primer campo
        Platform.runLater(() -> nameField.requestFocus());

        // Habilitar/deshabilitar botón Save según validación
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        
        // Validar que los campos de precio sean números
        Runnable validateFields = () -> {
            boolean valid = !nameField.getText().trim().isEmpty();
            try {
                if (!costPriceField.getText().trim().isEmpty()) {
                    Double.parseDouble(costPriceField.getText().trim());
                }
                if (!salePriceField.getText().trim().isEmpty()) {
                    Double.parseDouble(salePriceField.getText().trim());
                }
            } catch (NumberFormatException e) {
                valid = false;
            }
            saveButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        costPriceField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        salePriceField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());

        // Convertir resultado a Product actualizado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double costPrice = costPriceField.getText().trim().isEmpty() ? 0 : Double.parseDouble(costPriceField.getText().trim());
                    double salePrice = salePriceField.getText().trim().isEmpty() ? 0 : Double.parseDouble(salePriceField.getText().trim());
                    
                    return new Product(
                        product.getProductId(),
                        nameField.getText().trim(),
                        providerField.getText().trim(),
                        categoryField.getText().trim(),
                        descriptionField.getText().trim(),
                        costPrice,
                        salePrice,
                        quantitySpinner.getValue(),
                        product.getImagePath() // Mantener la imagen actual
                    );
                } catch (NumberFormatException e) {
                    showAlert("Invalid price format");
                    return null;
                }
            }
            return null;
        });

        // Mostrar diálogo y procesar resultado
        java.util.Optional<Product> result = dialog.showAndWait();
        result.ifPresent(updatedProduct -> {
            if (updatedProduct != null) {
                updateProductInBackend(updatedProduct);
            }
        });
    }

    /**
     * Envía el producto actualizado al backend
     */
    private void updateProductInBackend(Product product) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/products/" + product.getProductId());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                // Construir JSON
                String json = String.format(
                    "{\"productName\":\"%s\",\"provider\":\"%s\",\"category\":\"%s\",\"description\":\"%s\",\"costPrice\":%s,\"price\":%s,\"productQuantity\":%d,\"imagePath\":\"%s\"}",
                    escapeJson(product.getProductName()),
                    escapeJson(product.getProvider()),
                    escapeJson(product.getCategory()),
                    escapeJson(product.getDescription()),
                    product.getCostPrice(),
                    product.getPrice(),
                    product.getProductQuantity(),
                    escapeJson(product.getImagePath() != null ? product.getImagePath().replace("\\", "/") : "")
                );

                System.out.println("Updating product with JSON: " + json);

                // Enviar datos
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes("UTF-8"));
                }

                int responseCode = conn.getResponseCode();
                System.out.println("Update response code: " + responseCode);

                Platform.runLater(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        showInfo("Product updated successfully!");
                        loadProducts(); // Recargar tabla
                    } else {
                        showAlert("Error updating product. Response code: " + responseCode);
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Cannot connect to server: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Escapa caracteres especiales para JSON
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showImagePopup() {
        if (selectedProduct == null || table_image.getImage() == null) {
            return;
        }
        
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle(selectedProduct.getProductName());
        
        ImageView largeImage = new ImageView(table_image.getImage());
        largeImage.setFitWidth(600);
        largeImage.setFitHeight(500);
        largeImage.setPreserveRatio(true);
        
        Label nameLabel = new Label(selectedProduct.getProductName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 30;");
        closeBtn.setOnAction(e -> popupStage.close());
        
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: white;");
        layout.getChildren().addAll(nameLabel, largeImage, closeBtn);
        
        Scene scene = new Scene(layout);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }
}
