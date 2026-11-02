package com.inventory.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API routes to index.html so React Router works
 * when the frontend is embedded inside Spring Boot.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/login",
        "/add-product",
        "/inventory",
        "/clients",
        "/sales",
        "/analytics",
        "/installations",
        "/calendar",
        "/activity-log",
        "/invoices"
    })
    public String index() {
        return "forward:/index.html";
    }
}
