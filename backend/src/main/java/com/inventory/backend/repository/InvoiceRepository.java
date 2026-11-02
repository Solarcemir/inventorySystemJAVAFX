package com.inventory.backend.repository;

import com.inventory.backend.model.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // The items arrive in the same query, not in one extra query per invoice
    @EntityGraph(attributePaths = "items")
    List<Invoice> findByStatus(String status);

    @EntityGraph(attributePaths = "items")
    List<Invoice> findBySellerUsername(String sellerUsername);

    @EntityGraph(attributePaths = "items")
    List<Invoice> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    // Serializes status changes: a double click cannot finalize an invoice twice
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);
}
