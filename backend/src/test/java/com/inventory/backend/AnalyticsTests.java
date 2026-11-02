package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.inventory.backend.model.Product;
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

    @Test
    void midnightBelongsToThatDayAndFutureServicesAreNotToday() {
        LocalDateTime midnight = LocalDate.now().atStartOfDay();
        data.installation("At midnight", "10.00", "COMPLETED", midnight);
        data.installation("Tomorrow", "500.00", "COMPLETED", midnight.plusDays(1));

        assertThat(amount(analytics.getSummary(), "todaySales")).isEqualByComparingTo("10.00");
        assertThat(amount(today(analytics.getSalesByDay()), "total")).isEqualByComparingTo("10.00");
    }
}
