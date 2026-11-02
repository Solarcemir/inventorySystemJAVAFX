package com.inventory.backend.controller;

import com.inventory.backend.auth.SessionUser;
import com.inventory.backend.model.Installation;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.services.InstallationServiceBean;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/installations")
public class InstallationController {

    private final InstallationServiceBean installationService;

    public InstallationController(InstallationServiceBean installationService) {
        this.installationService = installationService;
    }

    @GetMapping
    public List<Installation> getAll() {
        return installationService.getAll();
    }

    @GetMapping("/{id}")
    public Installation getById(@PathVariable Long id) {
        return installationService.getById(id);
    }

    @PostMapping
    public Installation create(@RequestBody Installation installation) {
        return installationService.create(installation);
    }

    @PutMapping("/{id}")
    public Installation update(@PathVariable Long id, @RequestBody Installation installation) {
        return installationService.update(id, installation);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        installationService.delete(id);
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public List<Installation> getByInvoiceId(@PathVariable Long invoiceId) {
        return installationService.getByInvoiceId(invoiceId);
    }

    @PostMapping("/{id}/invoice")
    public Invoice createInvoice(@PathVariable Long id, HttpServletRequest request) {
        return installationService.createInvoice(id, SessionUser.from(request).username());
    }
}
