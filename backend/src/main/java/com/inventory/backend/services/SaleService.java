package com.inventory.backend.services;

import com.inventory.backend.model.Product;
import com.inventory.backend.model.Sale;
import com.inventory.backend.repository.ProductRepository;
import com.inventory.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    public Sale createSale(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        
        // Verificar stock disponible
        if (product.getProductQuantity() < quantity) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + product.getProductQuantity());
        }
        
        // Calcular total
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        
        // Reducir inventario
        product.setProductQuantity(product.getProductQuantity() - quantity);
        productRepository.save(product);
        
        // Crear venta
        Sale sale = new Sale(product, quantity, totalAmount, LocalDateTime.now());
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
}
