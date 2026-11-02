package com.inventory.backend.controller;

import com.inventory.backend.model.CalendarNote;
import com.inventory.backend.repository.CalendarNoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar-notes")
public class CalendarNoteController {

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
        return repo.save(new CalendarNote(body.get("dateKey"), body.get("text")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
