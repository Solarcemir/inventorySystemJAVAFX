package com.inventory.backend.controller;

import com.inventory.backend.services.AnalyticsService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Resumen general con todas las métricas principales
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return analyticsService.getSummary();
    }

    // Ventas por día (últimos 7 días)
    @GetMapping("/sales-by-day")
    public List<Map<String, Object>> getSalesByDay() {
        return analyticsService.getSalesByDay();
    }

    // Ventas por mes (últimos 6 meses)
    @GetMapping("/sales-by-month")
    public List<Map<String, Object>> getSalesByMonth() {
        return analyticsService.getSalesByMonth();
    }

    // Top 5 productos más vendidos
    @GetMapping("/top-products")
    public List<Map<String, Object>> getTopProducts() {
        return analyticsService.getTopProducts();
    }

    // Ventas por categoría
    @GetMapping("/sales-by-category")
    public List<Map<String, Object>> getSalesByCategory() {
        return analyticsService.getSalesByCategory();
    }

    // Productos con bajo stock (menos de 5 unidades)
    @GetMapping("/low-stock")
    public List<Map<String, Object>> getLowStockProducts() {
        return analyticsService.getLowStockProducts();
    }

    // Ventas recientes (últimas 10)
    @GetMapping("/recent-sales")
    public List<Map<String, Object>> getRecentSales() {
        return analyticsService.getRecentSales();
    }
    
    // Analytics diarios - 1 row por día con revenue, cost, profit, etc.
    @GetMapping("/daily")
    public List<Map<String, Object>> getDailyAnalytics() {
        return analyticsService.getDailyAnalytics();
    }
}
