package com.inventory.backend.repository;

import com.inventory.backend.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface ProductRepository extends JpaRepository<Product,Long>{

   // this will be auto
   Product findProductByProductName(String productName);

   // Obtener solo productos activos (no eliminados)
   List<Product> findByDeletedFalseOrDeletedIsNull();

   // Contar solo productos activos
   long countByDeletedFalseOrDeletedIsNull();

   // Bloquea la fila hasta terminar la transaccion: dos ventas simultaneas no pueden sobrevender
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select p from Product p where p.id = :id")
   Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
