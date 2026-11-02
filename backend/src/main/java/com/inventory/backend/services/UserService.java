package com.inventory.backend.services;

import com.inventory.backend.model.User;
import com.inventory.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./0-9A-Za-z]{53}$");

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Autowired
    private UserRepository userRepository;

  public User findValidUser(String username, String password) {
    User user = userRepository.findByUsername(username);
    if (user == null || user.getPassword() == null || password == null) {
      return null;
    }

    String stored = user.getPassword();
    if (BCRYPT_HASH.matcher(stored).matches()) {
      return passwordEncoder.matches(password, stored) ? user : null;
    }

    // A password still in plain text is replaced by its hash at the first correct login
    if (stored.equals(password)) {
      hashStoredPassword(user);
      return user;
    }
    return null;
  }

  // A failed upgrade never blocks the login: the next correct login tries again
  private void hashStoredPassword(User user) {
    try {
      String stored = user.getPassword();
      userRepository.replacePassword(user.getId(), stored, passwordEncoder.encode(stored));
    } catch (RuntimeException e) {
      log.warn("Could not hash the password of user {}: {}", user.getUsername(), e.getClass().getSimpleName());
    }
  }
}
