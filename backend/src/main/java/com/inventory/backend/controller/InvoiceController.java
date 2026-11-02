package com.inventory.backend.controller;

import com.inventory.backend.auth.AdminOnly;
import com.inventory.backend.auth.SessionUser;
import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.services.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<Invoice> getAll() {
        return invoiceService.getAll();
    }

    @GetMapping("/open")
    public List<Invoice> getOpen() {
        return invoiceService.getOpen();
    }

    @GetMapping("/{id}")
    public Invoice getById(@PathVariable Long id) {
        return invoiceService.getById(id);
    }

    @PostMapping
    public Invoice create(@RequestBody Invoice details, HttpServletRequest request) {
        // Only descriptive fields come from the browser: items, totals and status belong to the server
        Invoice invoice = new Invoice();
        invoice.setInvoiceName(details.getInvoiceName());
        invoice.setClientName(details.getClientName());
        invoice.setInvoiceNumber(details.getInvoiceNumber());
        invoice.setCarModel(details.getCarModel());
        invoice.setSellerUsername(SessionUser.from(request).username());
        return invoiceService.create(invoice);
    }

    @PostMapping("/{id}/items")
    public Invoice addItem(@PathVariable Long id, @RequestParam Long productId, @RequestParam int quantity) {
        return invoiceService.addItem(id, productId, quantity);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public Invoice removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return invoiceService.removeItem(id, itemId);
    }

    @PutMapping("/{id}/settings")
    public Invoice updateSettings(@PathVariable Long id, @RequestBody Map<String, String> settings,
                                  HttpServletRequest request) {
        String invoiceName = settings.get("invoiceName");
        String clientName = settings.get("clientName");
        BigDecimal discount = parseDiscount(settings.get("discountPercent"));
        if (!SessionUser.from(request).isAdmin()) {
            invoiceService.checkSellerMayEdit(id, discount);
        }
        String invoiceNumber = settings.get("invoiceNumber");
        String carModel = settings.get("carModel");
        return invoiceService.updateSettings(id, invoiceName, clientName, discount, invoiceNumber, carModel);
    }

    private BigDecimal parseDiscount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Descuento invalido: " + value);
        }
    }

    @PostMapping("/{id}/finalize")
    public Invoice finalizeInvoice(@PathVariable Long id) {
        return invoiceService.finalize(id);
    }

    @AdminOnly
    @PostMapping("/{id}/reopen")
    public Invoice reopen(@PathVariable Long id) {
        return invoiceService.reopen(id);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id) {
        invoiceService.cancel(id);
    }

    @AdminOnly
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        invoiceService.delete(id);
    }

    @GetMapping("/seller/{username}")
    public List<Invoice> getBySeller(@PathVariable String username) {
        return invoiceService.getBySeller(username);
    }
}
