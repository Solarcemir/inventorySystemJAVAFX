package com.inventory.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action")
    private String action; // SALE, ADD_PRODUCT, DELETE_PRODUCT, EDIT_PRODUCT, ADD_CLIENT, DELETE_SALE, INSTALLATION, LOGIN, INVOICE

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "username")
    private String username;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    public ActivityLog() {}

    public ActivityLog(String action, String description, String username) {
        this.action = action;
        this.description = description;
        this.username = username;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
