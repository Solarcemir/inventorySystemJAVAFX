package com.inventory.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    private int quantity;
    
    private BigDecimal totalAmount;
    
    @Column(name = "cost_amount")
    private BigDecimal costAmount;  // Costo total de la venta (costPrice * quantity)
    
    @Column(name = "profit_amount")
    private BigDecimal profitAmount;  // Ganancia de la venta (totalAmount - costAmount)

    private LocalDateTime saleDate;

    public Sale() {}

    public Sale(Product product, int quantity, BigDecimal totalAmount, LocalDateTime saleDate) {
        this.product = product;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.saleDate = saleDate;
    }
    
    public Sale(Product product, int quantity, BigDecimal totalAmount, BigDecimal costAmount, BigDecimal profitAmount, LocalDateTime saleDate) {
        this.product = product;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.costAmount = costAmount;
        this.profitAmount = profitAmount;
        this.saleDate = saleDate;
    }
    
    // Constructor completo con cliente
    public Sale(Product product, Client client, int quantity, BigDecimal totalAmount, BigDecimal costAmount, BigDecimal profitAmount, LocalDateTime saleDate) {
        this.product = product;
        this.client = client;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.costAmount = costAmount;
        this.profitAmount = profitAmount;
        this.saleDate = saleDate;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
}
