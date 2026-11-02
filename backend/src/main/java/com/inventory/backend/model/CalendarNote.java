package com.inventory.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "calendar_notes")
public class CalendarNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_key", nullable = false)  // "YYYY-MM-DD"
    private String dateKey;

    @Column(nullable = false)
    private String text;

    public CalendarNote() {}

    public CalendarNote(String dateKey, String text) {
        this.dateKey = dateKey;
        this.text = text;
    }

    public Long getId() { return id; }
    public String getDateKey() { return dateKey; }
    public String getText() { return text; }
}
