package com.inventory.backend.repository;

import com.inventory.backend.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
  // Obtener solo clientes activos (no eliminados)
  List<Client> findByDeletedFalseOrDeletedIsNull();

  // Contar solo clientes activos
  long countByDeletedFalseOrDeletedIsNull();

  Client findFirstByOrderBySpentAmountDesc();

  // La suma ocurre en la base de datos: dos ventas simultaneas no pueden pisarse el total
  @Modifying
  @Transactional
  @Query("update Client c set c.spentAmount = coalesce(c.spentAmount, 0) + :amount where c.id = :id")
  void addToSpentAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
