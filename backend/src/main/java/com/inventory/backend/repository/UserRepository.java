package com.inventory.backend.repository;


import com.inventory.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository <User,Long> {
  User findByUsername(String username);
} 
  
