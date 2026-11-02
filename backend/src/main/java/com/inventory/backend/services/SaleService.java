package com.inventory.backend.services;

import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.Client;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.Sale;
import com.inventory.backend.repository.ClientRepository;
import com.inventory.backend.repository.ProductRepository;
import com.inventory.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final ActivityLogService logService;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
                       ClientRepository clientRepository, ActivityLogService logService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.clientRepository = clientRepository;
        this.logService = logService;
    }

    /**
     * Crear una venta SIN cliente (mantiene compatibilidad con el método anterior)
     */
    @Transactional
    public Sale createSale(Long productId, int quantity) {
        return createSaleWithClient(productId, null, quantity);
    }

    /**
     * Crear una venta CON cliente - actualiza automáticamente el spentAmount del cliente
     */
    @Transactional
    public Sale createSaleWithClient(Long productId, Long clientId, int quantity) {
        if (quantity <= 0) {
            throw ApiException.badRequest("La cantidad debe ser mayor a 0");
        }

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        // Verificar stock disponible
        if (product.getProductQuantity() < quantity) {
            throw ApiException.conflict("Stock insuficiente. Disponible: " + product.getProductQuantity());
        }

        // Calcular total (precio de venta)
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        // Calcular costo y ganancia
        BigDecimal costAmount = BigDecimal.ZERO;
        if (product.getCostPrice() != null) {
            costAmount = product.getCostPrice().multiply(BigDecimal.valueOf(quantity));
        }
        BigDecimal profitAmount = totalAmount.subtract(costAmount);

        // Reducir inventario
        product.setProductQuantity(product.getProductQuantity() - quantity);
        productRepository.save(product);

        // Obtener cliente si se proporcionó
        Client client = null;
        if (clientId != null) {
            client = clientRepository.findById(clientId)
                    .orElseThrow(() -> ApiException.notFound("Cliente no encontrado"));

            // Actualizar el monto gastado del cliente
            BigDecimal currentSpent = client.getSpentAmount() != null ? client.getSpentAmount() : BigDecimal.ZERO;
            client.setSpentAmount(currentSpent.add(totalAmount));
            clientRepository.save(client);
        }

        // Crear venta con cliente, costo y ganancia
        Sale sale = new Sale(product, client, quantity, totalAmount, costAmount, profitAmount, LocalDateTime.now());
        Sale saved = saleRepository.save(sale);
        logService.log("SALE", "Sold " + quantity + "x \"" + product.getProductName() + "\" for $"
                + totalAmount.setScale(2, RoundingMode.HALF_UP));
        return saved;
    }

    public long getTotalSalesCount() {
        return saleRepository.count();
    }

    public long getSalesCountByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
        return saleRepository.countByProduct(product);
    }

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    /**
     * Obtener historial de compras de un cliente
     */
    public List<Sale> getSalesByClient(Long clientId) {
        return saleRepository.findByClientId(clientId);
    }

    /**
     * Contar ventas de un cliente
     */
    public long getSalesCountByClient(Long clientId) {
        return saleRepository.countByClientId(clientId);
    }

    /**
     * Eliminar una venta y devolver la cantidad al inventario.
     * Las ventas de una factura se revierten desde la factura: si se borraran aqui,
     * reabrir esa factura devolveria el stock una segunda vez.
     */
    @Transactional
    public void deleteSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> ApiException.notFound("Venta no encontrada"));

        if (sale.getInvoiceId() != null) {
            throw ApiException.conflict("Esta venta pertenece a la factura #" + sale.getInvoiceId()
                    + ". Para revertirla, reabre o elimina la factura.");
        }

        // Devolver cantidad al inventario del producto
        Product saleProduct = sale.getProduct();
        if (saleProduct != null) {
            productRepository.findByIdForUpdate(saleProduct.getId()).ifPresent(product -> {
                product.setProductQuantity(product.getProductQuantity() + sale.getQuantity());
                productRepository.save(product);
            });
        }

        // Revertir el monto gastado del cliente
        Client client = sale.getClient();
        if (client != null && sale.getTotalAmount() != null) {
            BigDecimal currentSpent = client.getSpentAmount() != null ? client.getSpentAmount() : BigDecimal.ZERO;
            client.setSpentAmount(currentSpent.subtract(sale.getTotalAmount()));
            clientRepository.save(client);
        }

        saleRepository.deleteById(saleId);
        logService.log("DELETE_SALE", "Deleted sale #" + saleId + " — " + sale.getQuantity() + "x \""
                + (saleProduct != null ? saleProduct.getProductName() : "N/A") + "\""
                + (sale.getTotalAmount() != null ? " ($" + sale.getTotalAmount().setScale(2, RoundingMode.HALF_UP) + ")" : ""));
    }
}
