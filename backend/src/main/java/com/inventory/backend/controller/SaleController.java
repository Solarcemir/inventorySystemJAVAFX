package com.inventory.backend.controller;

import com.inventory.backend.auth.AdminOnly;
import com.inventory.backend.auth.SessionUser;
import com.inventory.backend.model.Sale;
import com.inventory.backend.services.IdempotencyService;
import com.inventory.backend.services.SaleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/sales")
public class SaleController {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final SaleService saleService;
    private final IdempotencyService idempotencyService;

    public SaleController(SaleService saleService, IdempotencyService idempotencyService) {
        this.saleService = saleService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Sin clave la venta se ejecuta siempre. Con clave, un doble clic o un reintento
     * no vende dos veces. La clave vale solo para este usuario y esta misma venta.
     */
    private Sale sellOnce(String key, HttpServletRequest request, String sale, Supplier<Sale> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        return idempotencyService.runOnce(SessionUser.from(request).username() + ":" + sale + ":" + key, action);
    }

    /**
     * Crear venta SIN cliente (compatibilidad con versión anterior)
     */
    @PostMapping
    public Sale createSale(@RequestParam Long productId, @RequestParam int quantity,
                           @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String key,
                           HttpServletRequest request) {
        return sellOnce(key, request, productId + ":" + quantity, () -> saleService.createSale(productId, quantity));
    }
    
    /**
     * Crear venta CON cliente - actualiza automáticamente el spentAmount del cliente
     * Ejemplo: POST /api/sales/with-client?productId=1&clientId=1&quantity=2
     */
    @PostMapping("/with-client")
    public Sale createSaleWithClient(
            @RequestParam Long productId,
            @RequestParam Long clientId,
            @RequestParam int quantity,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String key,
            HttpServletRequest request) {
        return sellOnce(key, request, productId + ":" + clientId + ":" + quantity,
                () -> saleService.createSaleWithClient(productId, clientId, quantity));
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

    /**
     * Eliminar una venta y devolver la cantidad al inventario
     * Ejemplo: DELETE /api/sales/5
     */
    @AdminOnly
    @DeleteMapping("/{id}")
    public void deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
    }
}
