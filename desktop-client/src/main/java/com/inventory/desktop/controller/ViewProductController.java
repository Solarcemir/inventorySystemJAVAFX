package com.inventory.desktop.controller;

import com.inventory.desktop.model.Product;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
        detail_price.setText("Price: $" + String.format("%.2f", product.getPrice()));
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

        // Crear diálogo para ingresar cantidad a vender
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Sell Product");
        dialog.setHeaderText("Sell: " + selectedProduct.getProductName());
        dialog.setContentText("Quantity to sell (Available: " + selectedProduct.getProductQuantity() + "):");

        dialog.showAndWait().ifPresent(quantityStr -> {
            try {
                int quantity = Integer.parseInt(quantityStr);
                if (quantity < 1) {
                    showAlert("Quantity must be at least 1");
                    return;
                }
                if (quantity > selectedProduct.getProductQuantity()) {
                    showAlert("Not enough stock. Available: " + selectedProduct.getProductQuantity());
                    return;
                }
                createSale(selectedProduct.getProductId(), quantity);
            } catch (NumberFormatException e) {
                showAlert("Invalid quantity");
            }
        });
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
        confirmation.setContentText("This action cannot be undone.");

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
        
        // TODO: Implementar pantalla de edición
        showInfo("Edit functionality coming soon!\nSelected: " + selectedProduct.getProductName());
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
}
