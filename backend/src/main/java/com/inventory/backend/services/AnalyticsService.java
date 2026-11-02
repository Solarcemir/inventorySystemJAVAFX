package com.inventory.backend.services;

import com.inventory.backend.model.Installation;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.Sale;
import com.inventory.backend.repository.InstallationRepository;
import com.inventory.backend.repository.ProductRepository;
import com.inventory.backend.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
        List<Product> activeProducts = productRepository.findByDeletedFalseOrDeletedIsNull();
        long totalProducts = activeProducts.size();
        summary.put("totalProducts", totalProducts);
        
        // Total de ventas (cantidad)
        long totalSalesCount = saleRepository.count();
        summary.put("totalSalesCount", totalSalesCount);
        
        // Ingresos totales (revenue = precio venta)
        List<Sale> allSales = saleRepository.findAll();
        BigDecimal totalRevenue = allSales.stream()
                .map(Sale::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalRevenue", totalRevenue);
        
        // Calcular costo total y profit desde los campos de Sale
        BigDecimal totalCost = allSales.stream()
                .map(Sale::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalCost", totalCost);
        
        // Profit total directo desde ventas
        BigDecimal totalProfit = allSales.stream()
                .map(Sale::getProfitAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("totalProfit", totalProfit);
        
        // Margen de ganancia porcentual
        BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        summary.put("profitMargin", profitMargin);
        
        // Venta promedio
        BigDecimal avgSale = totalSalesCount > 0 
                ? totalRevenue.divide(BigDecimal.valueOf(totalSalesCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        summary.put("averageSale", avgSale);
        
        // Valor del inventario (solo productos activos, usando precio de venta)
        BigDecimal inventoryValue = activeProducts.stream()
                .filter(p -> p.getPrice() != null && p.getProductQuantity() != null && p.getProductQuantity() > 0)
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("inventoryValue", inventoryValue);
        
        // Costo del inventario (solo productos activos)
        BigDecimal inventoryCost = activeProducts.stream()
                .filter(p -> p.getCostPrice() != null && p.getProductQuantity() != null && p.getProductQuantity() > 0)
                .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(p.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("inventoryCost", inventoryCost);
        
        // Ganancia potencial del inventario
        BigDecimal potentialProfit = inventoryValue.subtract(inventoryCost);
        summary.put("potentialProfit", potentialProfit);
        
        // Productos con bajo stock (menos de 5) - solo activos
        long lowStockCount = activeProducts.stream()
                .filter(p -> p.getProductQuantity() != null && p.getProductQuantity() > 0 && p.getProductQuantity() < 5)
                .count();
        summary.put("lowStockCount", lowStockCount);
        
        // Productos sin stock - solo activos
        long outOfStockCount = activeProducts.stream()
                .filter(p -> p.getProductQuantity() == null || p.getProductQuantity() == 0)
                .count();
        summary.put("outOfStockCount", outOfStockCount);
        
        // Ventas de hoy
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfDay.plusDays(1);
        BigDecimal todaySales = allSales.stream()
                .filter(s -> inRange(s.getSaleDate(), startOfDay, endOfToday))
                .map(Sale::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("todaySales", todaySales);
        
        // Profit de hoy directo desde ventas
        BigDecimal todayProfit = allSales.stream()
                .filter(s -> inRange(s.getSaleDate(), startOfDay, endOfToday))
                .map(Sale::getProfitAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("todayProfit", todayProfit);

        // ── Installation revenue (COMPLETED only, price = pure revenue, no cost tracked) ──
        List<Installation> completedInstalls = installationRepository.findByStatus("COMPLETED");
        BigDecimal installationRevenue = completedInstalls.stream()
                .map(Installation::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("installationRevenue", installationRevenue);
        summary.put("installationCount", completedInstalls.size());

        // Today's installation revenue
        BigDecimal todayInstallRevenue = installationRevenue(completedInstalls, startOfDay, endOfToday);

        // Add installation revenue into the grand totals
        summary.put("totalRevenue", totalRevenue.add(installationRevenue));
        summary.put("totalProfit", totalProfit.add(installationRevenue)); // no cost for installations
        summary.put("todaySales", ((BigDecimal) summary.get("todaySales")).add(todayInstallRevenue));
        summary.put("todayProfit", todayProfit.add(todayInstallRevenue));

        // Recalculate profit margin with updated totals
        BigDecimal newRevenue = (BigDecimal) summary.get("totalRevenue");
        BigDecimal newProfit  = (BigDecimal) summary.get("totalProfit");
        BigDecimal newMargin  = newRevenue.compareTo(BigDecimal.ZERO) > 0
                ? newProfit.divide(newRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        summary.put("profitMargin", newMargin);

        return summary;
    }

    // Incluye el inicio y excluye el final, para que una venta a medianoche caiga en un solo día
    private boolean inRange(LocalDateTime when, LocalDateTime start, LocalDateTime end) {
        return when != null && !when.isBefore(start) && when.isBefore(end);
    }

    // Ingresos de instalaciones completadas en un rango (sin costo registrado)
    private BigDecimal installationRevenue(List<Installation> completedInstalls, LocalDateTime start, LocalDateTime end) {
        return completedInstalls.stream()
                .filter(i -> inRange(i.getServiceDate(), start, end))
                .map(Installation::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Ventas por día (últimos 7 días)
    public List<Map<String, Object>> getSalesByDay() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Sale> allSales = saleRepository.findAll();
        List<Installation> completedInstalls = installationRepository.findByStatus("COMPLETED");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            BigDecimal dayTotal = allSales.stream()
                    .filter(s -> inRange(s.getSaleDate(), startOfDay, endOfDay))
                    .map(Sale::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(installationRevenue(completedInstalls, startOfDay, endOfDay));

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(formatter));
            dayData.put("total", dayTotal);
            result.add(dayData);
        }
        
        return result;
    }

    // Ventas por mes (últimos 6 meses)
    public List<Map<String, Object>> getSalesByMonth() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Sale> allSales = saleRepository.findAll();
        List<Installation> completedInstalls = installationRepository.findByStatus("COMPLETED");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1);
            LocalDateTime startDateTime = monthStart.atStartOfDay();
            LocalDateTime endDateTime = monthEnd.atStartOfDay();
            
            BigDecimal monthTotal = allSales.stream()
                    .filter(s -> inRange(s.getSaleDate(), startDateTime, endDateTime))
                    .map(Sale::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(installationRevenue(completedInstalls, startDateTime, endDateTime));

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.format(formatter));
            monthData.put("total", monthTotal);
            result.add(monthData);
        }
        
        return result;
    }

    // Top 5 productos más vendidos
    public List<Map<String, Object>> getTopProducts() {
        List<Sale> allSales = saleRepository.findAll();
        
        Map<Long, Integer> productSales = new HashMap<>();
        Map<Long, String> productNames = new HashMap<>();
        Map<Long, BigDecimal> productRevenue = new HashMap<>();
        
        for (Sale sale : allSales) {
            if (sale.getProduct() != null) {
                Long productId = sale.getProduct().getId();
                String productName = sale.getProduct().getProductName();
                
                productSales.merge(productId, sale.getQuantity(), Integer::sum);
                productNames.put(productId, productName != null ? productName : "Unknown");
                productRevenue.merge(productId, 
                        sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO, 
                        BigDecimal::add);
            }
        }
        
        return productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("productId", entry.getKey());
                    productData.put("productName", productNames.get(entry.getKey()));
                    productData.put("quantitySold", entry.getValue());
                    productData.put("revenue", productRevenue.get(entry.getKey()));
                    return productData;
                })
                .collect(Collectors.toList());
    }

    // Ventas por categoría
    public List<Map<String, Object>> getSalesByCategory() {
        List<Sale> allSales = saleRepository.findAll();
        
        Map<String, BigDecimal> categoryRevenue = new HashMap<>();
        Map<String, Integer> categorySales = new HashMap<>();
        
        for (Sale sale : allSales) {
            if (sale.getProduct() != null) {
                String category = sale.getProduct().getCategory();
                if (category == null || category.isEmpty()) {
                    category = "Uncategorized";
                }
                
                categoryRevenue.merge(category, 
                        sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO, 
                        BigDecimal::add);
                categorySales.merge(category, sale.getQuantity(), Integer::sum);
            }
        }
        
        return categoryRevenue.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> catData = new HashMap<>();
                    catData.put("category", entry.getKey());
                    catData.put("revenue", entry.getValue());
                    catData.put("quantity", categorySales.get(entry.getKey()));
                    return catData;
                })
                .collect(Collectors.toList());
    }

    // Productos con bajo stock (solo activos)
    public List<Map<String, Object>> getLowStockProducts() {
        return productRepository.findByDeletedFalseOrDeletedIsNull().stream()
                .filter(p -> p.getProductQuantity() != null && p.getProductQuantity() < 5 && p.getProductQuantity() > 0)
                .sorted(Comparator.comparing(Product::getProductQuantity))
                .limit(10)
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
        return saleRepository.findAll().stream()
                .filter(s -> s.getSaleDate() != null)
                .sorted(Comparator.comparing(Sale::getSaleDate).reversed())
                .limit(10)
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
        List<Sale> allSales = saleRepository.findAll();
        List<Installation> completedInstalls = installationRepository.findByStatus("COMPLETED");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            // Filtrar ventas del día
            List<Sale> daySales = allSales.stream()
                    .filter(s -> inRange(s.getSaleDate(), startOfDay, endOfDay))
                    .collect(Collectors.toList());

            // Instalaciones completadas del día (ingreso sin costo registrado)
            BigDecimal dayInstallRevenue = installationRevenue(completedInstalls, startOfDay, endOfDay);

            // Revenue del día
            BigDecimal dayRevenue = daySales.stream()
                    .map(Sale::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(dayInstallRevenue);

            // Cost del día
            BigDecimal dayCost = daySales.stream()
                    .map(Sale::getCostAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Profit del día
            BigDecimal dayProfit = daySales.stream()
                    .map(Sale::getProfitAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(dayInstallRevenue);

            // Cantidad de ventas
            int salesCount = daySales.size();
            
            // Items vendidos
            int itemsSold = daySales.stream()
                    .mapToInt(Sale::getQuantity)
                    .sum();
            
            // Margen del día
            BigDecimal dayMargin = dayRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? dayProfit.divide(dayRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(formatter));
            dayData.put("revenue", dayRevenue);
            dayData.put("cost", dayCost);
            dayData.put("profit", dayProfit);
            dayData.put("margin", dayMargin);
            dayData.put("salesCount", salesCount);
            dayData.put("itemsSold", itemsSold);
            result.add(dayData);
        }
        
        return result;
    }
}
