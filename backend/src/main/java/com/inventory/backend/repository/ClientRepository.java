package com.inventory.backend.repository;

import com.inventory.backend.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
  // Obtener solo clientes activos (no eliminados)
  List<Client> findByDeletedFalseOrDeletedIsNull();

  // Contar solo clientes activos
  long countByDeletedFalseOrDeletedIsNull();

  Client findFirstByOrderBySpentAmountDesc();
}