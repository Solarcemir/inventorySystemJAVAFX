package com.inventory.desktop.model;

import javafx.beans.property.*;

public class Product {
    private final LongProperty productId;
    private final StringProperty productName;
    private final StringProperty provider;
    private final StringProperty category;
    private final StringProperty description;
    private final DoubleProperty price;
    private final IntegerProperty productQuantity;
    private final StringProperty imagePath;

    public Product() {
        this(0L, "", "", "", "", 0.0, 0, "");
    }

    public Product(Long productId, String productName, String provider, String category, 
                   String description, Double price, Integer productQuantity, String imagePath) {
        this.productId = new SimpleLongProperty(productId);
        this.productName = new SimpleStringProperty(productName);
        this.provider = new SimpleStringProperty(provider);
        this.category = new SimpleStringProperty(category);
        this.description = new SimpleStringProperty(description);
        this.price = new SimpleDoubleProperty(price);
        this.productQuantity = new SimpleIntegerProperty(productQuantity);
        this.imagePath = new SimpleStringProperty(imagePath);
    }

    // Product ID
    public long getProductId() { return productId.get(); }
    public void setProductId(long value) { productId.set(value); }
    public LongProperty productIdProperty() { return productId; }

    // Product Name
    public String getProductName() { return productName.get(); }
    public void setProductName(String value) { productName.set(value); }
    public StringProperty productNameProperty() { return productName; }

    // Provider
    public String getProvider() { return provider.get(); }
    public void setProvider(String value) { provider.set(value); }
    public StringProperty providerProperty() { return provider; }

    // Category
    public String getCategory() { return category.get(); }
    public void setCategory(String value) { category.set(value); }
    public StringProperty categoryProperty() { return category; }

    // Description
    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
    public StringProperty descriptionProperty() { return description; }

    // Price
    public double getPrice() { return price.get(); }
    public void setPrice(double value) { price.set(value); }
    public DoubleProperty priceProperty() { return price; }

    // Quantity
    public int getProductQuantity() { return productQuantity.get(); }
    public void setProductQuantity(int value) { productQuantity.set(value); }
    public IntegerProperty productQuantityProperty() { return productQuantity; }

    // Image Path
    public String getImagePath() { return imagePath.get(); }
    public void setImagePath(String value) { imagePath.set(value); }
    public StringProperty imagePathProperty() { return imagePath; }
}
