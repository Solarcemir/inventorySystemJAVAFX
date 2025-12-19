
package com.inventory.backend.controller;
import com.inventory.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {
  
  @Autowired
    private UserService UserService;

    @PostMapping("/login")
    public boolean login(@RequestBody Map<String,String> cretentials){
      String username = cretentials.get("username");
      String password = cretentials.get("password");
      return UserService.validateUser(username,password);
    }
}
