package com.inventory.backend.controller;

import com.inventory.backend.model.Sale;
import com.inventory.backend.services.SaleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {
    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public Sale createSale(@RequestParam Long productId, @RequestParam int quantity) {
        return saleService.createSale(productId, quantity);
    }

    @GetMapping("/count")
    public long getTotalSalesCount() {
        return saleService.getTotalSalesCount();
    }

    @GetMapping("/count/{productId}")
    public long getSalesCountByProduct(@PathVariable Long productId) {
        return saleService.getSalesCountByProduct(productId);
    }

    @GetMapping
    public List<Sale> getAllSales() {
        return saleService.getAllSales();
    }
}
