package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.inventory.backend.model.Client;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.Sale;
import com.inventory.backend.services.AnalyticsService;
import com.inventory.backend.services.InvoiceService;
import com.inventory.backend.services.SaleService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every query travels over the internet to Supabase, so these screens must not
 * issue more queries as the business accumulates products, clients and sales.
 */
@IsolatedDatabaseTest
class QueryCountTests {

    @Autowired TestData data;
    @Autowired AnalyticsService analytics;
    @Autowired SaleService sales;
    @Autowired InvoiceService invoices;
    @Autowired EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void reset() {
        data.clear();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private void addBusinessData(int size) {
        for (int i = 0; i < size; i++) {
            Product first = data.product("First " + i, "10.00", "4.00", 100);
            Product second = data.product("Second " + i, "20.00", "8.00", 100);
            Client client = data.client("Client", "Number " + i);
            sales.createSaleWithClient(first.getId(), client.getId(), 1);
            sales.createSale(second.getId(), 1);

            Invoice invoice = new Invoice();
            invoice.setClientName("Client Number " + i);
            Long id = invoices.create(invoice).getId();
            invoices.addItem(id, first.getId(), 1);
            invoices.addItem(id, second.getId(), 1);
            invoices.finalize(id);

            data.installation("Service " + i, "30.00", "COMPLETED", LocalDateTime.now());
        }
    }

    private long statementsFor(Runnable screen) {
        statistics.clear();
        screen.run();
        return statistics.getPrepareStatementCount();
    }

    private void analyticsScreen() {
        analytics.getSummary();
        analytics.getSalesByDay();
        analytics.getTopProducts();
        analytics.getSalesByCategory();
        analytics.getLowStockProducts();
        analytics.getRecentSales();
        analytics.getDailyAnalytics();
    }

    @Test
    void theAnalyticsScreenCostsTheSameWithMoreData() {
        addBusinessData(5);
        long small = statementsFor(this::analyticsScreen);

        addBusinessData(15);
        long large = statementsFor(this::analyticsScreen);

        assertThat(large).isEqualTo(small).isLessThanOrEqualTo(13);
    }

    @Test
    void theSalesListIsOneQuery() {
        addBusinessData(10);

        assertThat(statementsFor(sales::getAllSales)).isEqualTo(1);
    }

    @Test
    void theInvoiceListIsOneQuery() {
        addBusinessData(10);

        assertThat(statementsFor(invoices::getAll)).isEqualTo(1);
    }

    @Test
    void theSalesListStillCarriesProductAndClient() {
        addBusinessData(3);

        List<Sale> all = sales.getAllSales();

        assertThat(all).hasSize(12).allSatisfy(sale -> assertThat(sale.getProduct()).isNotNull());
        assertThat(all).filteredOn(sale -> sale.getClient() != null).hasSize(9);
    }

    @Test
    void theInvoiceListHasNoRepeatedInvoicesAndKeepsItsOrder() {
        addBusinessData(4);

        List<Invoice> all = invoices.getAll();

        assertThat(all).hasSize(4).doesNotHaveDuplicates().allSatisfy(invoice -> assertThat(invoice.getItems()).hasSize(2));
        assertThat(all).extracting(Invoice::getCreatedAt).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }
}
