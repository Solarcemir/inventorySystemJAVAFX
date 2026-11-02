package com.inventory.backend.controller;

import com.inventory.backend.auth.AdminOnly;
import com.inventory.backend.model.ActivityLog;
import com.inventory.backend.services.ActivityLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AdminOnly
@RestController
@RequestMapping("/api/logs")
public class ActivityLogController {

    private final ActivityLogService logService;

    public ActivityLogController(ActivityLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public List<ActivityLog> getAll() {
        return logService.getAll();
    }

    @GetMapping("/user/{username}")
    public List<ActivityLog> getByUser(@PathVariable String username) {
        return logService.getByUser(username);
    }

    @GetMapping("/action/{action}")
    public List<ActivityLog> getByAction(@PathVariable String action) {
        return logService.getByAction(action);
    }
}
