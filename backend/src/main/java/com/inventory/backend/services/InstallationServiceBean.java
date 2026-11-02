package com.inventory.backend.services;

import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.Installation;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.repository.InstallationRepository;
import com.inventory.backend.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InstallationServiceBean {

    private final InstallationRepository repo;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceService invoiceService;
    private final ActivityLogService logService;

    public InstallationServiceBean(InstallationRepository repo, InvoiceRepository invoiceRepo,
                                   InvoiceService invoiceService, ActivityLogService logService) {
        this.repo = repo;
        this.invoiceRepo = invoiceRepo;
        this.invoiceService = invoiceService;
        this.logService = logService;
    }

    public List<Installation> getAll() {
        return repo.findAllByOrderByServiceDateDesc();
    }

    public Installation getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Servicio de instalacion no encontrado"));
    }

    @Transactional
    public Installation create(Installation installation) {
        validate(installation);
        if (installation.getServiceDate() == null) {
            installation.setServiceDate(LocalDateTime.now());
        }
        if (installation.getStatus() == null) {
            installation.setStatus("PENDING");
        }
        Installation saved = repo.save(installation);
        logService.log("INSTALLATION", "Created installation \"" + saved.getServiceName() + "\""
                + (saved.getInvoiceId() != null ? " from invoice #" + saved.getInvoiceId() : ""));
        return saved;
    }

    @Transactional
    public Installation update(Long id, Installation details) {
        validate(details);
        Installation installation = getById(id);
        installation.setServiceName(details.getServiceName());
        installation.setClientName(details.getClientName());
        installation.setInstallerName(details.getInstallerName());
        installation.setDescription(details.getDescription());
        installation.setPrice(details.getPrice());
        installation.setStatus(details.getStatus());
        installation.setInvoiceNumber(details.getInvoiceNumber());
        installation.setCarModel(details.getCarModel());
        installation.setInvoiceId(details.getInvoiceId());
        if (details.getServiceDate() != null) {
            installation.setServiceDate(details.getServiceDate());
        }
        Installation saved = repo.save(installation);
        logService.log("INSTALLATION", "Edited installation \"" + saved.getServiceName() + "\" (ID: " + id + ")");
        return saved;
    }

    /**
     * Returns the invoice billing this installation, opening a new one when it has none.
     */
    @Transactional
    public Invoice createInvoice(Long id, String sellerUsername) {
        Installation installation = getById(id);
        if (installation.getInvoiceId() != null) {
            var existing = invoiceRepo.findById(installation.getInvoiceId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Invoice invoice = invoiceService.createForInstallation(installation, sellerUsername);
        installation.setInvoiceId(invoice.getId());
        repo.save(installation);
        return invoice;
    }

    public List<Installation> getByInvoiceId(Long invoiceId) {
        return repo.findByInvoiceId(invoiceId);
    }

    @Transactional
    public void delete(Long id) {
        repo.findById(id).ifPresent(installation -> {
            repo.delete(installation);
            logService.log("INSTALLATION", "Deleted installation \"" + installation.getServiceName() + "\" (ID: " + id + ")");
        });
    }

    public List<Installation> getByInstaller(String installerName) {
        return repo.findByInstallerName(installerName);
    }

    public List<Installation> getByStatus(String status) {
        return repo.findByStatus(status);
    }

    private void validate(Installation installation) {
        if (installation.getServiceName() == null || installation.getServiceName().isBlank()) {
            throw ApiException.badRequest("El nombre del servicio es obligatorio");
        }
        if (installation.getPrice() != null && installation.getPrice().signum() < 0) {
            throw ApiException.badRequest("El precio no puede ser negativo");
        }
    }
}
