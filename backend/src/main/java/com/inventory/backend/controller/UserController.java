
package com.inventory.backend.controller;
import com.inventory.backend.auth.PublicEndpoint;
import com.inventory.backend.auth.SessionUser;
import com.inventory.backend.model.User;
import com.inventory.backend.services.ActivityLogService;
import com.inventory.backend.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

  @Autowired
    private UserService UserService;

  @Autowired
    private ActivityLogService activityLogService;

    @PublicEndpoint
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String,String> credentials, HttpServletRequest request){
      String username = credentials.get("username");
      String password = credentials.get("password");
      User user = UserService.findValidUser(username, password);

      Map<String, Object> result = new HashMap<>();
      if (user != null) {
        endSession(request); // A new login always gets a new session id
        request.getSession(true).setAttribute(SessionUser.SESSION_KEY, new SessionUser(user.getUsername(), user.getRole()));
        activityLogService.log("LOGIN", "User \"" + user.getUsername() + "\" logged in (" + user.getRole() + ")", user.getUsername());

        result.put("success", true);
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("id", user.getId());
      } else {
        result.put("success", false);
      }
      return result;
    }

    @PublicEndpoint
    @PostMapping("/logout")
    public void logout(HttpServletRequest request){
      endSession(request);
    }

    private void endSession(HttpServletRequest request){
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
    }
}
