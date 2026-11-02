package com.inventory.backend.repository;

import com.inventory.backend.model.CalendarNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalendarNoteRepository extends JpaRepository<CalendarNote, Long> {
    List<CalendarNote> findByDateKey(String dateKey);
}
