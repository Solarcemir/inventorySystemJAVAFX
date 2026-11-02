package com.inventory.backend.services;

import com.inventory.backend.repository.InstallationRepository;
import com.inventory.backend.repository.ProductRepository;
import com.inventory.backend.repository.SaleRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every number here is calculated by the database. Loading sales into memory to add
 * them up costs one query per product and client, over the internet to Supabase.
 */
@Service
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final InstallationRepository installationRepository;

    public AnalyticsService(ProductRepository productRepository, SaleRepository saleRepository,
                            InstallationRepository installationRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.installationRepository = installationRepository;
    }

    // Resumen general
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Solo productos activos (no eliminados)
        ProductRepository.InventoryTotals inventory = productRepository.inventoryTotals();
        summary.put("totalProducts", inventory.getTotalProducts());
        summary.put("inventoryValue", inventory.getInventoryValue());
        summary.put("inventoryCost", inventory.getInventoryCost());
        summary.put("potentialProfit", inventory.getInventoryValue().subtract(inventory.getInventoryCost()));
        summary.put("lowStockCount", inventory.getLowStockCount());
        summary.put("outOfStockCount", inventory.getOutOfStockCount());

        // Ventas de productos
        SaleRepository.Totals sales = saleRepository.totals();
        summary.put("totalSalesCount", sales.getSalesCount());
        summary.put("totalCost", sales.getCost());

        // Venta promedio (sin instalaciones)
        BigDecimal avgSale = sales.getSalesCount() > 0
                ? sales.getRevenue().divide(BigDecimal.valueOf(sales.getSalesCount()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        summary.put("averageSale", avgSale);

        // ── Installation revenue (COMPLETED only, price = pure revenue, no cost tracked) ──
        InstallationRepository.RevenueTotals installations = installationRepository.completedTotals();
        summary.put("installationRevenue", installations.getRevenue());
        summary.put("installationCount", installations.getServices());

        // Add installation revenue into the grand totals
        BigDecimal totalRevenue = sales.getRevenue().add(installations.getRevenue());
        BigDecimal totalProfit = sales.getProfit().add(installations.getRevenue()); // no cost for installations
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalProfit", totalProfit);

        // Margen de ganancia porcentual
        summary.put("profitMargin", margin(totalProfit, totalRevenue));

        // Ventas de hoy
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfDay.plusDays(1);
        SaleRepository.Totals today = saleRepository.totalsBetween(startOfDay, endOfToday);
        BigDecimal todayInstallRevenue = installationRepository.completedTotalsBetween(startOfDay, endOfToday).getRevenue();
        summary.put("todaySales", today.getRevenue().add(todayInstallRevenue));
        summary.put("todayProfit", today.getProfit().add(todayInstallRevenue));

        return summary;
    }

    private BigDecimal margin(BigDecimal profit, BigDecimal revenue) {
        return revenue.compareTo(BigDecimal.ZERO) > 0
                ? profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }

    // Ingresos de instalaciones completadas por día (sin costo registrado)
    private Map<LocalDate, BigDecimal> installationRevenueByDay(LocalDateTime start, LocalDateTime end) {
        return installationRepository.completedRevenueByDay(start, end).stream()
                .collect(Collectors.toMap(InstallationRepository.DailyRevenue::getDay,
                        InstallationRepository.DailyRevenue::getRevenue));
    }

    // Ingresos por día: ventas más instalaciones completadas
    private Map<LocalDate, BigDecimal> revenueByDay(LocalDateTime start, LocalDateTime end) {
        Map<LocalDate, BigDecimal> revenue = new HashMap<>(installationRevenueByDay(start, end));
        for (SaleRepository.DailyTotals day : saleRepository.dailyTotals(start, end)) {
            revenue.merge(day.getDay(), day.getRevenue(), BigDecimal::add);
        }
        return revenue;
    }

    // Ventas por día (últimos 7 días)
    public List<Map<String, Object>> getSalesByDay() {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        LocalDate today = LocalDate.now();
        Map<LocalDate, BigDecimal> revenue = revenueByDay(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(formatter));
            dayData.put("total", revenue.getOrDefault(date, BigDecimal.ZERO));
            result.add(dayData);
        }

        return result;
    }

    // Ventas por mes (últimos 6 meses)
    public List<Map<String, Object>> getSalesByMonth() {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        Map<LocalDate, BigDecimal> revenue = revenueByDay(thisMonth.minusMonths(5).atStartOfDay(), thisMonth.plusMonths(1).atStartOfDay());

        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = thisMonth.minusMonths(i);
            LocalDate monthEnd = monthStart.plusMonths(1);

            BigDecimal monthTotal = revenue.entrySet().stream()
                    .filter(day -> !day.getKey().isBefore(monthStart) && day.getKey().isBefore(monthEnd))
                    .map(Map.Entry::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.format(formatter));
            monthData.put("total", monthTotal);
            result.add(monthData);
        }

        return result;
    }

    // Top 5 productos más vendidos
    public List<Map<String, Object>> getTopProducts() {
        return saleRepository.topProducts(Limit.of(5)).stream()
                .map(row -> {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("productId", row.getProductId());
                    productData.put("productName", row.getProductName() != null ? row.getProductName() : "Unknown");
                    productData.put("quantitySold", Math.toIntExact(row.getQuantitySold()));
                    productData.put("revenue", row.getRevenue());
                    return productData;
                })
                .collect(Collectors.toList());
    }

    // Ventas por categoría
    public List<Map<String, Object>> getSalesByCategory() {
        return saleRepository.categoryTotals().stream()
                .map(row -> {
                    Map<String, Object> catData = new HashMap<>();
                    catData.put("category", row.getCategory());
                    catData.put("revenue", row.getRevenue());
                    catData.put("quantity", Math.toIntExact(row.getQuantity()));
                    return catData;
                })
                .collect(Collectors.toList());
    }

    // Productos con bajo stock (solo activos)
    public List<Map<String, Object>> getLowStockProducts() {
        return productRepository.findLowStock(Limit.of(10)).stream()
                .map(p -> {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("productId", p.getId());
                    productData.put("productName", p.getProductName());
                    productData.put("quantity", p.getProductQuantity());
                    productData.put("category", p.getCategory());
                    return productData;
                })
                .collect(Collectors.toList());
    }

    // Ventas recientes
    public List<Map<String, Object>> getRecentSales() {
        return saleRepository.findTop10BySaleDateIsNotNullOrderBySaleDateDesc().stream()
                .map(s -> {
                    Map<String, Object> saleData = new HashMap<>();
                    saleData.put("saleId", s.getId());
                    saleData.put("productName", s.getProduct() != null ? s.getProduct().getProductName() : "Unknown");
                    saleData.put("quantity", s.getQuantity());
                    saleData.put("totalAmount", s.getTotalAmount());
                    saleData.put("saleDate", s.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    return saleData;
                })
                .collect(Collectors.toList());
    }

    // Analytics diarios - 1 row por día con todas las métricas
    public List<Map<String, Object>> getDailyAnalytics() {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Map<LocalDate, SaleRepository.DailyTotals> salesByDay = saleRepository.dailyTotals(start, end).stream()
                .collect(Collectors.toMap(SaleRepository.DailyTotals::getDay, Function.identity()));
        Map<LocalDate, BigDecimal> installationsByDay = installationRevenueByDay(start, end);

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            SaleRepository.DailyTotals daySales = salesByDay.get(date);

            // Instalaciones completadas del día (ingreso sin costo registrado)
            BigDecimal dayInstallRevenue = installationsByDay.getOrDefault(date, BigDecimal.ZERO);

            BigDecimal dayRevenue = dayInstallRevenue.add(daySales != null ? daySales.getRevenue() : BigDecimal.ZERO);
            BigDecimal dayCost = daySales != null ? daySales.getCost() : BigDecimal.ZERO;
            BigDecimal dayProfit = dayInstallRevenue.add(daySales != null ? daySales.getProfit() : BigDecimal.ZERO);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(formatter));
            dayData.put("revenue", dayRevenue);
            dayData.put("cost", dayCost);
            dayData.put("profit", dayProfit);
            dayData.put("margin", margin(dayProfit, dayRevenue));
            dayData.put("salesCount", daySales != null ? Math.toIntExact(daySales.getSalesCount()) : 0);
            dayData.put("itemsSold", daySales != null ? Math.toIntExact(daySales.getItemsSold()) : 0);
            result.add(dayData);
        }

        return result;
    }
}
