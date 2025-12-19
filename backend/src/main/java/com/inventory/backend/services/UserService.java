package com.inventory.backend.services;

import com.inventory.backend.model.User;
import com.inventory.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  @Autowired
    private UserRepository userRepository;

  public boolean validateUser(String username, String password){
    User user = userRepository.findByUsername(username);
    return user !=null && user.getPassword().equals((password));
  }

}
