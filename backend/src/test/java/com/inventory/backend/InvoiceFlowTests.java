package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.Client;
import com.inventory.backend.model.Installation;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.Sale;
import com.inventory.backend.services.InstallationServiceBean;
import com.inventory.backend.services.InvoiceService;
import com.inventory.backend.services.SaleService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IsolatedDatabaseTest
class InvoiceFlowTests {

    @Autowired TestData data;
    @Autowired InvoiceService invoices;
    @Autowired SaleService sales;
    @Autowired InstallationServiceBean installations;

    @BeforeEach
    void reset() {
        data.clear();
    }

    private Invoice newInvoice() {
        Invoice invoice = new Invoice();
        invoice.setSellerUsername("seller");
        return invoices.create(invoice);
    }

    private Invoice setDiscount(Invoice invoice, String percent) {
        return invoices.updateSettings(invoice.getId(), null, null, new BigDecimal(percent), null, null);
    }

    private Invoice setClient(Invoice invoice, String name) {
        return invoices.updateSettings(invoice.getId(), null, name, null, null, null);
    }

    private BigDecimal sum(List<Sale> list, java.util.function.Function<Sale, BigDecimal> field) {
        return list.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void discountReachesRevenueAndProfitButTaxDoesNot() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Product cable = data.product("Cable", "33.33", "10.00", 10);
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 1);
        invoices.addItem(invoice.getId(), cable.getId(), 3);
        setDiscount(invoice, "10");

        Invoice finalized = invoices.finalize(invoice.getId());

        // Subtotal 199.99, discount 20.00: the shop earned 179.99 before ITBMS
        assertThat(finalized.getSubtotal()).isEqualByComparingTo("199.99");
        assertThat(finalized.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(finalized.getTotal()).isEqualByComparingTo("192.59");

        List<Sale> recorded = data.sales.findByInvoiceId(invoice.getId());
        assertThat(recorded).hasSize(2);
        assertThat(sum(recorded, Sale::getTotalAmount)).isEqualByComparingTo("179.99");
        assertThat(sum(recorded, Sale::getCostAmount)).isEqualByComparingTo("90.00");
        assertThat(sum(recorded, Sale::getProfitAmount)).isEqualByComparingTo("89.99");
    }

    @Test
    void discountRoundingNeverLosesACent() {
        Product a = data.product("A", "0.01", "0.00", 10);
        Product b = data.product("B", "0.01", "0.00", 10);
        Product c = data.product("C", "33.33", "0.00", 10);
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), a.getId(), 1);
        invoices.addItem(invoice.getId(), b.getId(), 1);
        invoices.addItem(invoice.getId(), c.getId(), 1);
        setDiscount(invoice, "33.3");

        Invoice finalized = invoices.finalize(invoice.getId());

        BigDecimal expected = finalized.getSubtotal().subtract(finalized.getDiscountAmount());
        assertThat(sum(data.sales.findByInvoiceId(invoice.getId()), Sale::getTotalAmount)).isEqualByComparingTo(expected);
    }

    @Test
    void finalizingAnInvoiceWorthZeroDoesNotFail() {
        Product gift = data.product("Gift", "0.00", "0.00", 5);
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), gift.getId(), 1);
        invoices.addItem(invoice.getId(), gift.getId(), 1);
        setDiscount(invoice, "10");

        assertThat(invoices.finalize(invoice.getId()).getStatus()).isEqualTo("FINALIZED");
        assertThat(data.stock(gift)).isEqualTo(3);
    }

    @Test
    void reopenRestoresStockOnceAndRemovesTheSales() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 4);
        invoices.finalize(invoice.getId());
        assertThat(data.stock(speaker)).isEqualTo(6);

        Invoice reopened = invoices.reopen(invoice.getId());

        assertThat(reopened.getStatus()).isEqualTo("OPEN");
        assertThat(data.stock(speaker)).isEqualTo(10);
        assertThat(data.sales.findByInvoiceId(invoice.getId())).isEmpty();
    }

    @Test
    void aSaleFromAnInvoiceCannotBeDeletedOnItsOwn() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 4);
        invoices.finalize(invoice.getId());
        Sale sale = data.sales.findByInvoiceId(invoice.getId()).get(0);

        assertThatThrownBy(() -> sales.deleteSale(sale.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("factura");

        // Reversing through the invoice still returns the units exactly once
        assertThat(data.stock(speaker)).isEqualTo(6);
        invoices.reopen(invoice.getId());
        assertThat(data.stock(speaker)).isEqualTo(10);
    }

    @Test
    void aDirectSaleCanStillBeDeleted() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Client client = data.client("Ana", "Gomez");
        Sale sale = sales.createSaleWithClient(speaker.getId(), client.getId(), 2);

        sales.deleteSale(sale.getId());

        assertThat(data.stock(speaker)).isEqualTo(10);
        assertThat(data.clients.findById(client.getId()).orElseThrow().getSpentAmount()).isEqualByComparingTo("0");
    }

    @Test
    void walkInSaleLowersStock() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);

        Sale sale = sales.createSale(speaker.getId(), 3);

        assertThat(sale.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(data.stock(speaker)).isEqualTo(7);
    }

    @Test
    void zeroAndNegativeQuantitiesAreRejected() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Invoice invoice = newInvoice();

        for (int quantity : new int[] {0, -1, -50}) {
            assertThatThrownBy(() -> sales.createSale(speaker.getId(), quantity)).isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> invoices.addItem(invoice.getId(), speaker.getId(), quantity)).isInstanceOf(ApiException.class);
        }

        assertThat(data.stock(speaker)).isEqualTo(10);
        assertThat(data.sales.count()).isZero();
    }

    @Test
    void aFailedFinalizeChangesNothing() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Product scarce = data.product("Scarce", "50.00", "20.00", 5);
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 2);
        invoices.addItem(invoice.getId(), scarce.getId(), 4);
        invoices.addItem(invoice.getId(), scarce.getId(), 4); // Together they exceed the stock

        assertThatThrownBy(() -> invoices.finalize(invoice.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(data.stock(speaker)).isEqualTo(10);
        assertThat(data.stock(scarce)).isEqualTo(5);
        assertThat(data.sales.count()).isZero();
        assertThat(invoices.getById(invoice.getId()).getStatus()).isEqualTo("OPEN");
    }

    @Test
    void finalizedInvoicesCannotBeCancelledOrHaveTheirDiscountChanged() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 1);
        setDiscount(invoice, "10");
        invoices.finalize(invoice.getId());

        assertThatThrownBy(() -> invoices.cancel(invoice.getId())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> setDiscount(invoice, "20")).isInstanceOf(ApiException.class);

        // The settings screen resends the current discount when renaming a finalized invoice
        assertThat(setDiscount(invoice, "10.00").getStatus()).isEqualTo("FINALIZED");
    }

    @Test
    void discountMustBeBetweenZeroAndOneHundred() {
        Invoice invoice = newInvoice();

        assertThatThrownBy(() -> setDiscount(invoice, "-1")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> setDiscount(invoice, "100.5")).isInstanceOf(ApiException.class);
    }

    @Test
    void invoiceSalesCountTowardsAMatchingClient() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Client ana = data.client("Ana", "Gomez");
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 2);
        setClient(invoice, "  ana GOMEZ ");
        setDiscount(invoice, "10");

        invoices.finalize(invoice.getId());

        assertThat(data.clients.findById(ana.getId()).orElseThrow().getSpentAmount()).isEqualByComparingTo("180.00");
        assertThat(data.sales.findByInvoiceId(invoice.getId()).get(0).getClient().getId()).isEqualTo(ana.getId());

        invoices.reopen(invoice.getId());

        assertThat(data.clients.findById(ana.getId()).orElseThrow().getSpentAmount()).isEqualByComparingTo("0");
    }

    @Test
    void deletingAFinalizedInvoiceRevertsTheClientTotal() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Client ana = data.client("Ana", "Gomez");
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 1);
        setClient(invoice, "Ana Gomez");
        invoices.finalize(invoice.getId());

        invoices.delete(invoice.getId());

        assertThat(data.clients.findById(ana.getId()).orElseThrow().getSpentAmount()).isEqualByComparingTo("0");
        assertThat(data.stock(speaker)).isEqualTo(10);
    }

    @Test
    void anAmbiguousClientNameIsNotLinked() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Client first = data.client("Ana", "Gomez");
        Client second = data.client("Ana", "Gomez");
        Invoice invoice = newInvoice();
        invoices.addItem(invoice.getId(), speaker.getId(), 1);
        setClient(invoice, "Ana Gomez");

        invoices.finalize(invoice.getId());

        assertThat(data.sales.findByInvoiceId(invoice.getId()).get(0).getClient()).isNull();
        assertThat(data.clients.findById(first.getId()).orElseThrow().getSpentAmount()).isEqualByComparingTo("0");
        assertThat(data.clients.findById(second.getId()).orElseThrow().getSpentAmount()).isEqualByComparingTo("0");
    }

    @Test
    void atMostFiveInvoicesCanBeOpen() {
        for (int i = 0; i < 5; i++) {
            newInvoice();
        }

        assertThatThrownBy(this::newInvoice)
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Maximo 5");
    }

    @Test
    void billingAnInstallationCreatesALinkedInvoiceWithItsPrice() {
        Installation installation = data.installation("Radio", "100.00", "PENDING", LocalDateTime.now());

        Invoice invoice = installations.createInvoice(installation.getId(), "seller");

        assertThat(invoice.getStatus()).isEqualTo("OPEN");
        assertThat(invoice.getSellerUsername()).isEqualTo("seller");
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getItems().get(0).getProductId()).isNull();
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(invoice.getTotal()).isEqualByComparingTo("107.00");
        assertThat(installations.getById(installation.getId()).getInvoiceId()).isEqualTo(invoice.getId());
    }

    @Test
    void billingTheSameInstallationTwiceReturnsTheSameInvoice() {
        Installation installation = data.installation("Radio", "100.00", "PENDING", LocalDateTime.now());

        Invoice first = installations.createInvoice(installation.getId(), "seller");
        Invoice second = installations.createInvoice(installation.getId(), "seller");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(data.invoices.count()).isEqualTo(1);
    }

    @Test
    void serviceLinesCreateNoSaleAndMoveNoStock() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Installation installation = data.installation("Radio", "50.00", "COMPLETED", LocalDateTime.now());
        Invoice invoice = installations.createInvoice(installation.getId(), "seller");
        invoices.addItem(invoice.getId(), speaker.getId(), 1);

        Invoice finalized = invoices.finalize(invoice.getId());

        assertThat(finalized.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(data.sales.findByInvoiceId(invoice.getId())).hasSize(1);
        assertThat(data.stock(speaker)).isEqualTo(9);

        invoices.reopen(invoice.getId());

        assertThat(data.stock(speaker)).isEqualTo(10);
        assertThat(invoices.getById(invoice.getId()).getItems()).hasSize(2);
    }

    @Test
    void deletingAnInvoiceKeepsItsInstallationsUnlinked() {
        Installation installation = data.installation("Radio", "50.00", "COMPLETED", LocalDateTime.now());
        Invoice invoice = installations.createInvoice(installation.getId(), "seller");
        invoices.finalize(invoice.getId());

        invoices.delete(invoice.getId());

        assertThat(installations.getById(installation.getId()).getInvoiceId()).isNull();
    }

    @Test
    void cancellingAnInvoiceFreesItsInstallation() {
        Installation installation = data.installation("Radio", "50.00", "PENDING", LocalDateTime.now());
        Invoice invoice = installations.createInvoice(installation.getId(), "seller");

        invoices.cancel(invoice.getId());

        assertThat(installations.getById(installation.getId()).getInvoiceId()).isNull();
    }

    @Test
    void removingAServiceLineFreesItsInstallation() {
        Installation installation = data.installation("Radio", "50.00", "PENDING", LocalDateTime.now());
        Invoice invoice = installations.createInvoice(installation.getId(), "seller");

        Invoice updated = invoices.removeItem(invoice.getId(), invoice.getItems().get(0).getId());

        assertThat(updated.getItems()).isEmpty();
        assertThat(updated.getTotal()).isEqualByComparingTo("0");
        assertThat(installations.getById(installation.getId()).getInvoiceId()).isNull();
    }
}
