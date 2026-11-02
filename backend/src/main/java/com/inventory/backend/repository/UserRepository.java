package com.inventory.backend.repository;


import com.inventory.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository <User,Long> {
  User findByUsername(String username);

  // Only replaces the value it was given, so a password changed in the meantime is left alone
  @Modifying
  @Transactional
  @Query("update User u set u.password = :hashed where u.id = :id and u.password = :current")
  int replacePassword(@Param("id") Long id, @Param("current") String current, @Param("hashed") String hashed);
}

