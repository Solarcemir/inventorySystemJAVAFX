package com.inventory.backend.repository;

import com.inventory.backend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface ProductRepository extends JpaRepository<Product,Long>{

   // this will be auto
   Product findProductByProductName(String productName);
   
   // Obtener solo productos activos (no eliminados)
   List<Product> findByDeletedFalseOrDeletedIsNull();
   
   // Contar solo productos activos
   long countByDeletedFalseOrDeletedIsNull();
}
