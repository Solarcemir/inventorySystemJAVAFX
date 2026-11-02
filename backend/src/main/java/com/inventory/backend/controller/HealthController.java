package com.inventory.backend.controller;

import com.inventory.backend.auth.PublicEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @PublicEndpoint
  @GetMapping("/api/health")
  public String health() {
    return "backend {health} is running";
  }

}
