package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.inventory.backend.model.Product;
import com.inventory.backend.model.Sale;
import com.inventory.backend.services.AnalyticsService;
import com.inventory.backend.services.SaleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IsolatedDatabaseTest
class AnalyticsTests {

    @Autowired TestData data;
    @Autowired AnalyticsService analytics;
    @Autowired SaleService sales;

    @BeforeEach
    void reset() {
        data.clear();
    }

    private Map<String, Object> today(List<Map<String, Object>> days) {
        return days.get(days.size() - 1);
    }

    private BigDecimal amount(Map<String, Object> row, String key) {
        return (BigDecimal) row.get(key);
    }

    // The frontend types in types/index.ts rely on these exact keys
    @Test
    void topProductsAndCategoriesKeepTheirJsonKeys() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        sales.createSale(speaker.getId(), 3);

        Map<String, Object> top = analytics.getTopProducts().get(0);
        assertThat(top).containsKeys("productId", "productName", "quantitySold", "revenue");
        assertThat(top.get("quantitySold")).isEqualTo(3);
        assertThat(amount(top, "revenue")).isEqualByComparingTo("300.00");

        Map<String, Object> category = analytics.getSalesByCategory().get(0);
        assertThat(category).containsKeys("category", "revenue", "quantity");
        assertThat(amount(category, "revenue")).isEqualByComparingTo("300.00");
    }

    @Test
    void completedInstallationsAppearInEveryViewOfTheDay() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        sales.createSale(speaker.getId(), 1);
        data.installation("Radio", "50.00", "COMPLETED", LocalDateTime.now());
        data.installation("Pending", "999.00", "PENDING", LocalDateTime.now());

        assertThat(amount(analytics.getSummary(), "todaySales")).isEqualByComparingTo("150.00");
        assertThat(amount(today(analytics.getSalesByDay()), "total")).isEqualByComparingTo("150.00");
        assertThat(amount(today(analytics.getSalesByMonth()), "total")).isEqualByComparingTo("150.00");

        Map<String, Object> daily = today(analytics.getDailyAnalytics());
        assertThat(amount(daily, "revenue")).isEqualByComparingTo("150.00");
        assertThat(amount(daily, "cost")).isEqualByComparingTo("60.00");
        assertThat(amount(daily, "profit")).isEqualByComparingTo("90.00");
        assertThat(amount(daily, "margin")).isEqualByComparingTo("60.00");
        assertThat(daily.get("salesCount")).isEqualTo(1);
    }

    private Product incomplete(String name, String price, String cost, Integer quantity, Boolean deleted) {
        Product product = new Product();
        product.setProductName(name);
        product.setPrice(price != null ? new BigDecimal(price) : null);
        product.setCostPrice(cost != null ? new BigDecimal(cost) : null);
        product.setProductQuantity(quantity);
        product.setDeleted(deleted);
        return data.products.save(product);
    }

    @Test
    void inventoryTotalsSkipIncompleteProducts() {
        incomplete("Complete", "10.00", "4.00", 2, false);
        incomplete("No price", null, "5.00", 1, false);
        incomplete("No cost", "7.00", null, 10, null);
        incomplete("No units", "1.00", "1.00", 0, false);
        incomplete("Unknown units", "1.00", "1.00", null, false);
        incomplete("Negative units", "3.00", "3.00", -2, false);
        incomplete("Deleted", "100.00", "100.00", 100, true);

        Map<String, Object> summary = analytics.getSummary();

        assertThat(summary.get("totalProducts")).isEqualTo(6L);
        assertThat(amount(summary, "inventoryValue")).isEqualByComparingTo("90.00");
        assertThat(amount(summary, "inventoryCost")).isEqualByComparingTo("13.00");
        assertThat(amount(summary, "potentialProfit")).isEqualByComparingTo("77.00");
        assertThat(summary.get("lowStockCount")).isEqualTo(2L);
        assertThat(summary.get("outOfStockCount")).isEqualTo(2L);
    }

    @Test
    void anEmptyBusinessShowsZeros() {
        Map<String, Object> summary = analytics.getSummary();

        assertThat(summary.get("totalSalesCount")).isEqualTo(0L);
        assertThat(amount(summary, "totalRevenue")).isEqualByComparingTo("0");
        assertThat(amount(summary, "profitMargin")).isEqualByComparingTo("0");
        assertThat(amount(summary, "averageSale")).isEqualByComparingTo("0");
        assertThat(analytics.getTopProducts()).isEmpty();
        assertThat(analytics.getSalesByCategory()).isEmpty();
        assertThat(analytics.getDailyAnalytics()).hasSize(7);
    }

    @Test
    void salesWithMissingFieldsCountOnlyWhereTheyCan() {
        data.sales.save(new Sale(null, 2, new BigDecimal("30.00"), null, null, null));

        Map<String, Object> summary = analytics.getSummary();

        assertThat(summary.get("totalSalesCount")).isEqualTo(1L);
        assertThat(amount(summary, "totalRevenue")).isEqualByComparingTo("30.00");
        assertThat(amount(summary, "totalCost")).isEqualByComparingTo("0");
        assertThat(amount(summary, "averageSale")).isEqualByComparingTo("30.00");
        assertThat(amount(summary, "todaySales")).isEqualByComparingTo("0");
        assertThat(analytics.getTopProducts()).isEmpty();
        assertThat(analytics.getRecentSales()).isEmpty();
    }

    @Test
    void categoriesMergeTheEmptyOnesAndSortByRevenue() {
        Product noCategory = incomplete("A", "10.00", "1.00", 50, false);
        Product blank = incomplete("B", "10.00", "1.00", 50, false);
        blank.setCategory("");
        data.products.save(blank);
        Product audio = data.product("C", "100.00", "1.00", 50);
        sales.createSale(noCategory.getId(), 1);
        sales.createSale(blank.getId(), 2);
        sales.createSale(audio.getId(), 1);

        List<Map<String, Object>> categories = analytics.getSalesByCategory();

        assertThat(categories).hasSize(2);
        assertThat(categories.get(0).get("category")).isEqualTo("Audio");
        assertThat(categories.get(1).get("category")).isEqualTo("Uncategorized");
        assertThat(amount(categories.get(1), "revenue")).isEqualByComparingTo("30.00");
        assertThat(categories.get(1).get("quantity")).isEqualTo(3);
    }

    @Test
    void topProductsRankByUnitsAndKeepOnlyFive() {
        for (int i = 1; i <= 7; i++) {
            sales.createSale(data.product("Product " + i, "10.00", "1.00", 50).getId(), i);
        }

        List<Map<String, Object>> top = analytics.getTopProducts();

        assertThat(top).extracting(row -> row.get("productName"))
                .containsExactly("Product 7", "Product 6", "Product 5", "Product 4", "Product 3");
        assertThat(top.get(0).get("quantitySold")).isEqualTo(7);
    }

    @Test
    void deletedProductsStayInTheSalesRanking() {
        Product gone = data.product("Discontinued", "10.00", "1.00", 50);
        sales.createSale(gone.getId(), 2);
        gone.setDeleted(true);
        data.products.save(gone);

        assertThat(analytics.getTopProducts()).extracting(row -> row.get("productName")).containsExactly("Discontinued");
    }

    @Test
    void lowStockShowsTheTenScarcestActiveProducts() {
        for (int i = 0; i < 12; i++) {
            incomplete("Low " + i, "1.00", "1.00", (i % 4) + 1, false);
        }
        incomplete("Deleted low", "1.00", "1.00", 1, true);
        incomplete("Plenty", "1.00", "1.00", 5, false);
        incomplete("None", "1.00", "1.00", 0, false);

        List<Map<String, Object>> lowStock = analytics.getLowStockProducts();

        assertThat(lowStock).hasSize(10)
                .extracting(row -> row.get("quantity"))
                .containsExactly(1, 1, 1, 2, 2, 2, 3, 3, 3, 4);
        assertThat(lowStock).extracting(row -> row.get("productName")).doesNotContain("Deleted low", "Plenty", "None");
    }

    @Test
    void recentSalesShowTheLatestTenWithNewestFirst() {
        Product product = data.product("Speaker", "10.00", "1.00", 50);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 12; i++) {
            data.sales.save(new Sale(product, i + 1, new BigDecimal("10.00"), now.minusHours(i)));
        }
        data.sales.save(new Sale(product, 99, new BigDecimal("10.00"), null));

        List<Map<String, Object>> recent = analytics.getRecentSales();

        assertThat(recent).hasSize(10);
        assertThat(recent.get(0).get("quantity")).isEqualTo(1);
        assertThat(recent.get(9).get("quantity")).isEqualTo(10);
        assertThat(recent.get(0).get("productName")).isEqualTo("Speaker");
    }

    @Test
    void salesByMonthAddUpAcrossDays() {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        data.installation("Start of month", "10.00", "COMPLETED", firstOfMonth.atStartOfDay());
        data.installation("Today", "20.00", "COMPLETED", LocalDateTime.now());
        data.installation("Last month", "40.00", "COMPLETED", firstOfMonth.minusDays(1).atTime(23, 59));
        data.installation("Too old", "80.00", "COMPLETED", firstOfMonth.minusMonths(7).atStartOfDay());

        List<Map<String, Object>> months = analytics.getSalesByMonth();

        assertThat(months).hasSize(6);
        assertThat(amount(months.get(5), "total")).isEqualByComparingTo("30.00");
        assertThat(amount(months.get(4), "total")).isEqualByComparingTo("40.00");
        assertThat(amount(months.get(0), "total")).isEqualByComparingTo("0");
    }

    @Test
    void midnightBelongsToThatDayAndFutureServicesAreNotToday() {
        LocalDateTime midnight = LocalDate.now().atStartOfDay();
        data.installation("At midnight", "10.00", "COMPLETED", midnight);
        data.installation("Tomorrow", "500.00", "COMPLETED", midnight.plusDays(1));

        assertThat(amount(analytics.getSummary(), "todaySales")).isEqualByComparingTo("10.00");
        assertThat(amount(today(analytics.getSalesByDay()), "total")).isEqualByComparingTo("10.00");
    }
}
