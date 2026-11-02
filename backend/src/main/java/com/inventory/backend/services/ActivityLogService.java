package com.inventory.backend.services;

import com.inventory.backend.auth.SessionUser;
import com.inventory.backend.model.ActivityLog;
import com.inventory.backend.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
public class ActivityLogService {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ActivityLogRepository repo;

    public ActivityLogService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    /** Records an action under the user of the current request. */
    public void log(String action, String description) {
        log(action, description, currentUsername());
    }

    public void log(String action, String description, String username) {
        String text = description.length() > MAX_DESCRIPTION_LENGTH
                ? description.substring(0, MAX_DESCRIPTION_LENGTH)
                : description;
        repo.save(new ActivityLog(action, text, username));
    }

    private String currentUsername() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            SessionUser user = SessionUser.from(attributes.getRequest());
            if (user != null) {
                return user.username();
            }
        }
        return "system";
    }

    public List<ActivityLog> getAll() {
        return repo.findAllByOrderByTimestampDesc();
    }

    public List<ActivityLog> getByUser(String username) {
        return repo.findByUsernameOrderByTimestampDesc(username);
    }

    public List<ActivityLog> getByAction(String action) {
        return repo.findByActionOrderByTimestampDesc(action);
    }
}
