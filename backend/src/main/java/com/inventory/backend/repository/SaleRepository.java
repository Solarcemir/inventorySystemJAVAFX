package com.inventory.backend.repository;

import com.inventory.backend.model.Sale;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    long countByProduct(Product product);
    List<Sale> findByProduct(Product product);
    
    // Buscar ventas por cliente
    List<Sale> findByClient(Client client);
    List<Sale> findByClientId(Long clientId);
    
    // Contar ventas por cliente
    long countByClient(Client client);
    long countByClientId(Long clientId);
}
