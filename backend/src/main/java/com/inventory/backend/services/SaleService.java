package com.inventory.backend.services;

import com.inventory.backend.model.Client;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.Sale;
import com.inventory.backend.repository.ClientRepository;
import com.inventory.backend.repository.ProductRepository;
import com.inventory.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository, ClientRepository clientRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Crear una venta SIN cliente (mantiene compatibilidad con el método anterior)
     */
    public Sale createSale(Long productId, int quantity) {
        return createSaleWithClient(productId, null, quantity);
    }
    
    /**
     * Crear una venta CON cliente - actualiza automáticamente el spentAmount del cliente
     */
    @Transactional
    public Sale createSaleWithClient(Long productId, Long clientId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        
        // Verificar stock disponible
        if (product.getProductQuantity() < quantity) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + product.getProductQuantity());
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
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            
            // Actualizar el monto gastado del cliente
            BigDecimal currentSpent = client.getSpentAmount() != null ? client.getSpentAmount() : BigDecimal.ZERO;
            client.setSpentAmount(currentSpent.add(totalAmount));
            clientRepository.save(client);
        }
        
        // Crear venta con cliente, costo y ganancia
        Sale sale = new Sale(product, client, quantity, totalAmount, costAmount, profitAmount, LocalDateTime.now());
        return saleRepository.save(sale);
    }

    public long getTotalSalesCount() {
        return saleRepository.count();
    }

    public long getSalesCountByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
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
}
