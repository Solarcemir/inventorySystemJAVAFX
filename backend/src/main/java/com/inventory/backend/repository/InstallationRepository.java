package com.inventory.backend.repository;

import com.inventory.backend.model.Installation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallationRepository extends JpaRepository<Installation, Long> {
    List<Installation> findByInstallerName(String installerName);
    List<Installation> findByStatus(String status);
    List<Installation> findAllByOrderByServiceDateDesc();
    List<Installation> findByInvoiceId(Long invoiceId);
}
