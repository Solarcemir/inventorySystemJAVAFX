package com.inventory.backend.services;

import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.*;
import com.inventory.backend.repository.ClientRepository;
import com.inventory.backend.repository.InstallationRepository;
import com.inventory.backend.repository.InvoiceRepository;
import com.inventory.backend.repository.ProductRepository;
import com.inventory.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final ProductRepository productRepo;
    private final SaleRepository saleRepo;
    private final ClientRepository clientRepo;
    private final InstallationRepository installationRepo;
    private final ActivityLogService logService;

    public InvoiceService(InvoiceRepository invoiceRepo, ProductRepository productRepo, SaleRepository saleRepo,
                          ClientRepository clientRepo, InstallationRepository installationRepo,
                          ActivityLogService logService) {
        this.invoiceRepo = invoiceRepo;
        this.productRepo = productRepo;
        this.saleRepo = saleRepo;
        this.clientRepo = clientRepo;
        this.installationRepo = installationRepo;
        this.logService = logService;
    }

    public List<Invoice> getAll() {
        return invoiceRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<Invoice> getOpen() {
        return invoiceRepo.findByStatus("OPEN");
    }

    public Invoice getById(Long id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Factura no encontrada"));
    }

    private Invoice getForUpdate(Long id) {
        return invoiceRepo.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("Factura no encontrada"));
    }

    @Transactional
    public Invoice create(Invoice invoice) {
        long openCount = invoiceRepo.countByStatus("OPEN");
        if (openCount >= 5) {
            throw ApiException.conflict("Maximo 5 facturas abiertas a la vez");
        }
        invoice.setStatus("OPEN");
        invoice.setCreatedAt(LocalDateTime.now());
        Invoice saved = invoiceRepo.save(invoice);
        logService.log("INVOICE", "Created invoice #" + saved.getId());
        return saved;
    }

    /**
     * Opens an invoice billing an installation. The service becomes a line with no
     * product: it moves no stock and creates no Sale, because the installation's
     * revenue is already counted when it is COMPLETED.
     */
    @Transactional
    public Invoice createForInstallation(Installation installation, String sellerUsername) {
        Invoice invoice = new Invoice();
        invoice.setSellerUsername(sellerUsername);
        invoice.setInvoiceName("Inst: " + installation.getServiceName());
        invoice.setClientName(installation.getClientName());
        invoice.setCarModel(installation.getCarModel());
        invoice.setInvoiceNumber(installation.getInvoiceNumber());

        BigDecimal price = installation.getPrice() != null ? installation.getPrice() : BigDecimal.ZERO;
        InvoiceItem line = new InvoiceItem();
        line.setInvoice(invoice);
        line.setProductName(installation.getServiceName());
        line.setQuantity(1);
        line.setUnitPrice(price);
        line.setLineTotal(price);
        invoice.getItems().add(line);
        invoice.recalculate();

        return create(invoice);
    }

    @Transactional
    public Invoice addItem(Long invoiceId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw ApiException.badRequest("La cantidad debe ser mayor a 0");
        }

        Invoice invoice = getById(invoiceId);
        if (!"OPEN".equals(invoice.getStatus())) {
            throw ApiException.conflict("No se puede modificar una factura finalizada");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        if (product.getProductQuantity() < quantity) {
            throw ApiException.conflict("Stock insuficiente. Disponible: " + product.getProductQuantity());
        }

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setProductId(productId);
        item.setProductName(product.getProductName());
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));

        invoice.getItems().add(item);
        invoice.recalculate();
        return invoiceRepo.save(invoice);
    }

    @Transactional
    public Invoice removeItem(Long invoiceId, Long itemId) {
        Invoice invoice = getById(invoiceId);
        if (!"OPEN".equals(invoice.getStatus())) {
            throw ApiException.conflict("No se puede modificar una factura finalizada");
        }

        // Removing a service line frees its installation so it can be billed again
        invoice.getItems().stream()
                .filter(item -> item.getId().equals(itemId) && item.getProductId() == null)
                .findFirst()
                .ifPresent(line -> installationRepo.findByInvoiceId(invoiceId).stream()
                        .filter(installation -> line.getProductName().equals(installation.getServiceName()))
                        .forEach(installation -> {
                            installation.setInvoiceId(null);
                            installationRepo.save(installation);
                        }));

        invoice.getItems().removeIf(item -> item.getId().equals(itemId));
        invoice.recalculate();
        return invoiceRepo.save(invoice);
    }

    /** Sellers work on open invoices and leave the discount as it is. */
    public void checkSellerMayEdit(Long invoiceId, BigDecimal discountPercent) {
        Invoice invoice = getById(invoiceId);
        if (!"OPEN".equals(invoice.getStatus())) {
            throw ApiException.forbidden("Solo un administrador puede modificar una factura cerrada");
        }
        BigDecimal current = invoice.getDiscountPercent() != null ? invoice.getDiscountPercent() : BigDecimal.ZERO;
        if (discountPercent != null && discountPercent.compareTo(current) != 0) {
            throw ApiException.forbidden("Solo un administrador puede cambiar el descuento");
        }
    }

    @Transactional
    public Invoice updateSettings(Long invoiceId, String invoiceName, String clientName,
                                  BigDecimal discountPercent, String invoiceNumber, String carModel) {
        Invoice invoice = getById(invoiceId);
        if (discountPercent != null) {
            if (discountPercent.signum() < 0 || discountPercent.compareTo(new BigDecimal("100")) > 0) {
                throw ApiException.badRequest("El descuento debe estar entre 0 y 100");
            }
            // Finalized sales already carry the old discount
            BigDecimal current = invoice.getDiscountPercent() != null ? invoice.getDiscountPercent() : BigDecimal.ZERO;
            if (!"OPEN".equals(invoice.getStatus()) && discountPercent.compareTo(current) != 0) {
                throw ApiException.conflict("Reabre la factura para cambiar el descuento");
            }
            invoice.setDiscountPercent(discountPercent);
        }
        if (invoiceName != null) invoice.setInvoiceName(invoiceName);
        if (clientName != null) invoice.setClientName(clientName);
        if (invoiceNumber != null) invoice.setInvoiceNumber(invoiceNumber);
        if (carModel != null) invoice.setCarModel(carModel);
        invoice.recalculate();
        return invoiceRepo.save(invoice);
    }

    @Transactional
    public Invoice finalize(Long invoiceId) {
        Invoice invoice = getForUpdate(invoiceId);
        if (!"OPEN".equals(invoice.getStatus())) {
            throw ApiException.conflict("Solo se pueden finalizar facturas abiertas");
        }

        invoice.recalculate();
        Client client = findClientByName(invoice.getClientName());
        BigDecimal subtotal = invoice.getSubtotal();
        BigDecimal discount = invoice.getDiscountAmount();
        BigDecimal discountAllocated = BigDecimal.ZERO;
        BigDecimal clientTotal = BigDecimal.ZERO;

        List<InvoiceItem> items = invoice.getItems();
        for (int i = 0; i < items.size(); i++) {
            InvoiceItem item = items.get(i);

            // Each line carries its share of the discount; the last one takes the rounding remainder
            BigDecimal lineDiscount = BigDecimal.ZERO;
            if (i == items.size() - 1) {
                lineDiscount = discount.subtract(discountAllocated);
            } else if (subtotal.signum() > 0) {
                lineDiscount = discount.multiply(item.getLineTotal()).divide(subtotal, 2, RoundingMode.HALF_UP);
            }
            discountAllocated = discountAllocated.add(lineDiscount);

            // Service lines have no product, so no stock and no Sale
            if (item.getProductId() == null) {
                continue;
            }

            Product product = productRepo.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + item.getProductName()));
            if (product.getProductQuantity() < item.getQuantity()) {
                throw ApiException.conflict("Stock insuficiente para " + product.getProductName()
                        + ". Disponible: " + product.getProductQuantity());
            }
            product.setProductQuantity(product.getProductQuantity() - item.getQuantity());
            productRepo.save(product);

            BigDecimal costPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal cost    = costPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal revenue = item.getLineTotal().subtract(lineDiscount);
            BigDecimal profit  = revenue.subtract(cost);
            Sale sale = new Sale(product, client, item.getQuantity(), revenue, cost, profit, LocalDateTime.now());
            sale.setInvoiceId(invoiceId);
            saleRepo.save(sale);
            clientTotal = clientTotal.add(revenue);
        }

        if (client != null) {
            clientRepo.addToSpentAmount(client.getId(), clientTotal);
        }

        invoice.setStatus("FINALIZED");
        invoice.setFinalizedAt(LocalDateTime.now());
        Invoice saved = invoiceRepo.save(invoice);
        logService.log("INVOICE", "Finalized invoice #" + saved.getId() + " — Total: $"
                + saved.getTotal().setScale(2, RoundingMode.HALF_UP));
        return saved;
    }

    /**
     * Invoices store the client as free text, so the sale is linked only when the
     * name matches exactly one active client.
     */
    private Client findClientByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String wanted = name.trim();
        List<Client> matches = clientRepo.findByDeletedFalseOrDeletedIsNull().stream()
                .filter(c -> (c.getFirstName() + " " + c.getLastName()).trim().equalsIgnoreCase(wanted))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    /**
     * Reverses a finalized invoice: restores product quantities from invoice items
     * (reliable for all invoices regardless of whether sales had invoiceId set),
     * deletes any linked sale records, and sets the invoice back to OPEN.
     */
    @Transactional
    public Invoice reopen(Long invoiceId) {
        Invoice invoice = getForUpdate(invoiceId);
        if (!"FINALIZED".equals(invoice.getStatus())) {
            throw ApiException.conflict("Solo se pueden reabrir facturas finalizadas");
        }

        reverseFinalization(invoice);

        invoice.setStatus("OPEN");
        invoice.setFinalizedAt(null);
        Invoice saved = invoiceRepo.save(invoice);
        logService.log("INVOICE", "Reopened invoice #" + invoiceId + " for editing");
        return saved;
    }

    /**
     * Deletes an invoice. If it was FINALIZED, first reverses inventory and profit records.
     * Its installations stay on record, without the link to the deleted invoice.
     */
    @Transactional
    public void delete(Long invoiceId) {
        Invoice invoice = getForUpdate(invoiceId);
        if ("FINALIZED".equals(invoice.getStatus())) {
            reverseFinalization(invoice);
        }
        unlinkInstallations(invoiceId);
        invoiceRepo.deleteById(invoiceId);
        logService.log("INVOICE", "Deleted invoice #" + invoiceId);
    }

    private void reverseFinalization(Invoice invoice) {
        // Restore stock directly from invoice items — works for all invoices
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getProductId() == null) {
                continue;
            }
            productRepo.findByIdForUpdate(item.getProductId()).ifPresent(product -> {
                product.setProductQuantity(product.getProductQuantity() + item.getQuantity());
                productRepo.save(product);
            });
        }

        // Delete sale records linked to this invoice (present only when invoiceId tracking was active)
        List<Sale> linkedSales = saleRepo.findByInvoiceId(invoice.getId());
        for (Sale sale : linkedSales) {
            Client client = sale.getClient();
            if (client != null && sale.getTotalAmount() != null) {
                clientRepo.addToSpentAmount(client.getId(), sale.getTotalAmount().negate());
            }
        }
        if (!linkedSales.isEmpty()) {
            saleRepo.deleteAll(linkedSales);
        }
    }

    private void unlinkInstallations(Long invoiceId) {
        for (Installation installation : installationRepo.findByInvoiceId(invoiceId)) {
            installation.setInvoiceId(null);
            installationRepo.save(installation);
        }
    }

    @Transactional
    public void cancel(Long invoiceId) {
        Invoice invoice = getForUpdate(invoiceId);
        if (!"OPEN".equals(invoice.getStatus())) {
            throw ApiException.conflict("Solo se pueden cancelar facturas abiertas");
        }
        invoice.setStatus("CANCELLED");
        invoiceRepo.save(invoice);
        unlinkInstallations(invoiceId);
        logService.log("INVOICE", "Cancelled invoice #" + invoiceId);
    }

    public List<Invoice> getBySeller(String username) {
        return invoiceRepo.findBySellerUsername(username);
    }
}
