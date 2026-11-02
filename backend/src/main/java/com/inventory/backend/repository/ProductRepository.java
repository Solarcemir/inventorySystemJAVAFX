package com.inventory.backend.repository;

import com.inventory.backend.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@Repository
public interface ProductRepository extends JpaRepository<Product,Long>{

   interface InventoryTotals {
      long getTotalProducts();
      BigDecimal getInventoryValue();
      BigDecimal getInventoryCost();
      long getLowStockCount();
      long getOutOfStockCount();
   }

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

   // Totales de los productos activos, calculados por la base de datos
   @Query("""
         select count(p) as totalProducts,
                coalesce(sum(case when p.productQuantity > 0 then p.price * p.productQuantity end), 0) as inventoryValue,
                coalesce(sum(case when p.productQuantity > 0 then p.costPrice * p.productQuantity end), 0) as inventoryCost,
                coalesce(sum(case when p.productQuantity > 0 and p.productQuantity < 5 then 1 else 0 end), 0) as lowStockCount,
                coalesce(sum(case when p.productQuantity is null or p.productQuantity = 0 then 1 else 0 end), 0) as outOfStockCount
         from Product p
         where p.deleted = false or p.deleted is null
         """)
   InventoryTotals inventoryTotals();

   @Query("""
         select p from Product p
         where (p.deleted = false or p.deleted is null)
           and p.productQuantity > 0 and p.productQuantity < 5
         order by p.productQuantity, p.id
         """)
   List<Product> findLowStock(Limit limit);
}
