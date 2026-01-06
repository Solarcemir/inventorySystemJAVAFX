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

    /**
     * Crear venta SIN cliente (compatibilidad con versión anterior)
     */
    @PostMapping
    public Sale createSale(@RequestParam Long productId, @RequestParam int quantity) {
        return saleService.createSale(productId, quantity);
    }
    
    /**
     * Crear venta CON cliente - actualiza automáticamente el spentAmount del cliente
     * Ejemplo: POST /api/sales/with-client?productId=1&clientId=1&quantity=2
     */
    @PostMapping("/with-client")
    public Sale createSaleWithClient(
            @RequestParam Long productId, 
            @RequestParam Long clientId, 
            @RequestParam int quantity) {
        return saleService.createSaleWithClient(productId, clientId, quantity);
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
    
    /**
     * Obtener historial de compras de un cliente
     * Ejemplo: GET /api/sales/client/1
     */
    @GetMapping("/client/{clientId}")
    public List<Sale> getSalesByClient(@PathVariable Long clientId) {
        return saleService.getSalesByClient(clientId);
    }
    
    /**
     * Contar ventas de un cliente
     * Ejemplo: GET /api/sales/client/1/count
     */
    @GetMapping("/client/{clientId}/count")
    public long getSalesCountByClient(@PathVariable Long clientId) {
        return saleService.getSalesCountByClient(clientId);
    }
}
