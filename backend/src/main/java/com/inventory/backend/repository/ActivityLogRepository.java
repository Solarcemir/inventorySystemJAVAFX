package com.inventory.backend.repository;

import com.inventory.backend.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findAllByOrderByTimestampDesc();
    List<ActivityLog> findByUsernameOrderByTimestampDesc(String username);
    List<ActivityLog> findByActionOrderByTimestampDesc(String action);
}
