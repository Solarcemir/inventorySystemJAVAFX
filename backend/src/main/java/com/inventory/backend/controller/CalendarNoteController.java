package com.inventory.backend.controller;

import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.CalendarNote;
import com.inventory.backend.repository.CalendarNoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar-notes")
public class CalendarNoteController {

    private static final int MAX_TEXT_LENGTH = 255;

    private final CalendarNoteRepository repo;

    public CalendarNoteController(CalendarNoteRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<CalendarNote> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public CalendarNote create(@RequestBody Map<String, String> body) {
        String dateKey = body.get("dateKey");
        String text = body.get("text") != null ? body.get("text").trim() : "";
        try {
            LocalDate.parse(dateKey != null ? dateKey : "");
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Fecha no valida, se espera AAAA-MM-DD");
        }
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
            throw ApiException.badRequest("La nota debe tener entre 1 y " + MAX_TEXT_LENGTH + " caracteres");
        }
        return repo.save(new CalendarNote(dateKey, text));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
