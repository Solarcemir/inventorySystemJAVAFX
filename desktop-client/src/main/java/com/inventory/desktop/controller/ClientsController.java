package com.inventory.desktop.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import com.inventory.desktop.model.Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador para la vista de Clientes.
 * Maneja la tabla de clientes, búsqueda y comunicación con el backend.
 */
public class ClientsController implements Initializable {

    // ==================== Componentes FXML ====================
    @FXML
    private TableView<Client> clientsTable;

    @FXML
    private TableColumn<Client, Long> col_id;

    @FXML
    private TableColumn<Client, String> col_name;

    @FXML
    private TableColumn<Client, String> col_email;

    @FXML
    private TableColumn<Client, String> col_phone;

    @FXML
    private TableColumn<Client, String> col_address;

    @FXML
    private TextField searchField;

    @FXML
    private Button add_client;

    // ==================== Datos ====================
    private ObservableList<Client> clientsList = FXCollections.observableArrayList();
    private FilteredList<Client> filteredClients;

    // URL base de la API
    private static final String API_BASE_URL = "http://localhost:8080/api/clients";

    // ==================== Inicialización ====================
    /**
     * Método que se ejecuta automáticamente cuando se carga el FXML.
     * Aquí configuramos las columnas y cargamos los datos.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Configurar las columnas de la tabla
        setupTableColumns();

        // 2. Configurar la búsqueda/filtrado
        setupSearch();

        // 3. Cargar los clientes desde el backend
        loadClientsFromBackend();
    }

    // ==================== Configuración de columnas ====================
    /**
     * Configura cada columna para que sepa qué propiedad del objeto Client mostrar.
     * Usamos PropertyValueFactory que busca el método getXxx() o xxxProperty() en Client.
     */
    private void setupTableColumns() {
        // col_id mostrará el valor de client.getId()
        col_id.setCellValueFactory(new PropertyValueFactory<>("id"));

        // col_name mostrará el nombre completo (firstName + lastName)
        // Usamos una lambda para combinar ambos campos
        col_name.setCellValueFactory(cellData -> {
            Client client = cellData.getValue();
            String fullName = client.getFirstName() + " " + client.getLastName();
            return new javafx.beans.property.SimpleStringProperty(fullName);
        });

        // col_email mostrará client.getEmail()
        col_email.setCellValueFactory(new PropertyValueFactory<>("email"));

        // col_phone mostrará client.getPhoneNumber()
        col_phone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));

        // col_address - El backend no tiene address, usaremos spentAmount como placeholder
        // Puedes cambiar esto según tu modelo
        col_address.setCellValueFactory(cellData -> {
            Client client = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty("$" + String.format("%.2f", client.getSpentAmount()));
        });
        col_address.setText("Total Spent"); // Renombrar columna
    }

    // ==================== Búsqueda/Filtrado ====================
    /**
     * Configura el campo de búsqueda para filtrar la tabla en tiempo real.
     */
    private void setupSearch() {
        // Crear una lista filtrada basada en clientsList
        filteredClients = new FilteredList<>(clientsList, p -> true);

        // Cuando el texto del searchField cambie, actualizar el filtro
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredClients.setPredicate(client -> {
                // Si el campo está vacío, mostrar todos
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                // Buscar en nombre, email o teléfono
                if (client.getFirstName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (client.getLastName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (client.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (client.getPhoneNumber().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        // Asignar la lista filtrada a la tabla
        clientsTable.setItems(filteredClients);
    }

    // ==================== Cargar datos desde Backend ====================
    /**
     * Hace una petición HTTP GET al backend para obtener todos los clientes.
     * Se ejecuta en un hilo separado para no bloquear la UI.
     */
    private void loadClientsFromBackend() {
        new Thread(() -> {
            try {
                // 1. Crear la conexión HTTP
                URL url = new URL(API_BASE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                // 2. Verificar respuesta
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 3. Leer la respuesta JSON
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // 4. Parsear JSON y actualizar la UI
                    String jsonResponse = response.toString();
                    parseAndLoadClients(jsonResponse);
                } else {
                    Platform.runLater(() -> showError("Error al cargar clientes: HTTP " + responseCode));
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("No se pudo conectar al servidor: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Parsea el JSON de respuesta y carga los clientes en la lista.
     * Nota: Este es un parser simple. En producción usa Gson o Jackson.
     */
    private void parseAndLoadClients(String json) {
        // Limpiar lista actual
        Platform.runLater(() -> clientsList.clear());

        try {
            // Remover corchetes externos
            json = json.trim();
            if (json.startsWith("[")) {
                json = json.substring(1);
            }
            if (json.endsWith("]")) {
                json = json.substring(0, json.length() - 1);
            }

            // Si está vacío, no hay clientes
            if (json.isEmpty()) {
                return;
            }

            // Dividir por objetos (esto es simplificado, considera usar Gson/Jackson)
            String[] clientObjects = json.split("\\},\\s*\\{");

            for (String clientJson : clientObjects) {
                // Limpiar llaves
                clientJson = clientJson.replace("{", "").replace("}", "");

                // Extraer valores
                Long id = extractLong(clientJson, "id");
                String firstName = extractString(clientJson, "firstName");
                String lastName = extractString(clientJson, "lastName");
                String email = extractString(clientJson, "email");
                String phoneNumber = extractString(clientJson, "phoneNumber");
                Double spentAmount = extractDouble(clientJson, "spentAmount");

                Client client = new Client(id, firstName, lastName, email, phoneNumber, spentAmount);

                // Agregar a la lista en el hilo de UI
                Platform.runLater(() -> clientsList.add(client));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("Error al parsear datos: " + e.getMessage()));
        }
    }

    // ==================== Métodos auxiliares para parsear JSON ====================
    private String extractString(String json, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return "";

            start += pattern.length();
            // Saltar espacios
            while (start < json.length() && json.charAt(start) == ' ') start++;

            if (json.charAt(start) == '"') {
                start++;
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            } else if (json.substring(start).startsWith("null")) {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private Long extractLong(String json, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return 0L;

            start += pattern.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            return Long.parseLong(json.substring(start, end));
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double extractDouble(String json, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return 0.0;

            start += pattern.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                end++;
            }
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ==================== Acciones de botones ====================
    /**
     * Método que se ejecuta cuando se presiona el botón "Add Client".
     * Definido en el FXML con onAction="#add_client_button"
     */
    @FXML
    private void add_client_button() {
        showAddClientDialog();
    }

    /**
     * Muestra un diálogo para agregar un nuevo cliente
     */
    private void showAddClientDialog() {
        // Crear el diálogo
        javafx.scene.control.Dialog<Client> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Add New Client");
        dialog.setHeaderText("Enter client information");

        // Configurar botones
        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        // Crear campos del formulario
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");

        grid.add(new javafx.scene.control.Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new javafx.scene.control.Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new javafx.scene.control.Label("Phone:"), 0, 3);
        grid.add(phoneField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Enfocar el primer campo
        Platform.runLater(() -> firstNameField.requestFocus());

        // Habilitar/deshabilitar botón Save según validación
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Validar campos
        firstNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            saveButton.setDisable(newVal.trim().isEmpty() || lastNameField.getText().trim().isEmpty());
        });
        lastNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            saveButton.setDisable(newVal.trim().isEmpty() || firstNameField.getText().trim().isEmpty());
        });

        // Convertir resultado a Client
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new Client(
                    null,
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    0.0
                );
            }
            return null;
        });

        // Mostrar diálogo y procesar resultado
        java.util.Optional<Client> result = dialog.showAndWait();
        result.ifPresent(client -> {
            saveClientToBackend(client);
        });
    }

    /**
     * Envía el cliente al backend para guardarlo
     */
    private void saveClientToBackend(Client client) {
        new Thread(() -> {
            try {
                URL url = new URL(API_BASE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);

                // Construir JSON
                String json = String.format(
                    "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\",\"phoneNumber\":\"%s\"}",
                    escapeJson(client.getFirstName()),
                    escapeJson(client.getLastName()),
                    escapeJson(client.getEmail()),
                    escapeJson(client.getPhoneNumber())
                );

                System.out.println("Sending client JSON: " + json);

                // Enviar datos
                try (java.io.OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes("UTF-8"));
                }

                int responseCode = connection.getResponseCode();
                System.out.println("Response code: " + responseCode);

                Platform.runLater(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        showSuccess("Client added successfully!");
                        loadClientsFromBackend(); // Recargar tabla
                    } else {
                        showError("Error adding client. Response code: " + responseCode);
                    }
                });

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Cannot connect to server: " + e.getMessage()));
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

    /**
     * Método para refrescar la tabla (puedes conectarlo a un botón)
     */
    public void refreshTable() {
        loadClientsFromBackend();
    }

    // ==================== Utilidades ====================
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
