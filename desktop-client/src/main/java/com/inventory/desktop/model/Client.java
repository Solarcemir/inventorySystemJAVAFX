package com.inventory.desktop.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Modelo Client para el frontend JavaFX.
 * Usa JavaFX Properties para que la TableView pueda enlazar los datos automáticamente.
 */
public class Client {

    private final LongProperty id;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty email;
    private final StringProperty phoneNumber;
    private final DoubleProperty spentAmount;

    // Constructor vacío
    public Client() {
        this.id = new SimpleLongProperty();
        this.firstName = new SimpleStringProperty();
        this.lastName = new SimpleStringProperty();
        this.email = new SimpleStringProperty();
        this.phoneNumber = new SimpleStringProperty();
        this.spentAmount = new SimpleDoubleProperty();
    }

    // Constructor con parámetros
    public Client(Long id, String firstName, String lastName, String email, String phoneNumber, Double spentAmount) {
        this.id = new SimpleLongProperty(id != null ? id : 0);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.email = new SimpleStringProperty(email);
        this.phoneNumber = new SimpleStringProperty(phoneNumber);
        this.spentAmount = new SimpleDoubleProperty(spentAmount != null ? spentAmount : 0.0);
    }

    // ==================== ID ====================
    public Long getId() {
        return id.get();
    }

    public void setId(Long id) {
        this.id.set(id != null ? id : 0);
    }

    public LongProperty idProperty() {
        return id;
    }

    // ==================== First Name ====================
    public String getFirstName() {
        return firstName.get();
    }

    public void setFirstName(String firstName) {
        this.firstName.set(firstName);
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    // ==================== Last Name ====================
    public String getLastName() {
        return lastName.get();
    }

    public void setLastName(String lastName) {
        this.lastName.set(lastName);
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    // ==================== Email ====================
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public StringProperty emailProperty() {
        return email;
    }

    // ==================== Phone Number ====================
    public String getPhoneNumber() {
        return phoneNumber.get();
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber.set(phoneNumber);
    }

    public StringProperty phoneNumberProperty() {
        return phoneNumber;
    }

    // ==================== Spent Amount ====================
    public Double getSpentAmount() {
        return spentAmount.get();
    }

    public void setSpentAmount(Double spentAmount) {
        this.spentAmount.set(spentAmount != null ? spentAmount : 0.0);
    }

    public DoubleProperty spentAmountProperty() {
        return spentAmount;
    }

    // ==================== Método auxiliar para nombre completo ====================
    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + getId() +
                ", firstName='" + getFirstName() + '\'' +
                ", lastName='" + getLastName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", phoneNumber='" + getPhoneNumber() + '\'' +
                ", spentAmount=" + getSpentAmount() +
                '}';
    }
}


