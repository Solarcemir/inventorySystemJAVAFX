package com.inventory.backend.repository;

import com.inventory.backend.model.Sale;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.Client;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    interface Totals {
        BigDecimal getRevenue();
        BigDecimal getCost();
        BigDecimal getProfit();
        long getSalesCount();
        long getItemsSold();
    }

    interface DailyTotals extends Totals {
        LocalDate getDay();
    }

    interface TopProduct {
        Long getProductId();
        String getProductName();
        long getQuantitySold();
        BigDecimal getRevenue();
    }

    interface CategoryTotals {
        String getCategory();
        BigDecimal getRevenue();
        long getQuantity();
    }

    long countByProduct(Product product);
    List<Sale> findByProduct(Product product);

    // Producto y cliente llegan en la misma consulta, no en una consulta extra por cada uno
    @Override
    @EntityGraph(attributePaths = {"product", "client"})
    List<Sale> findAll();

    // Buscar ventas por cliente
    List<Sale> findByClient(Client client);

    @EntityGraph(attributePaths = {"product", "client"})
    List<Sale> findByClientId(Long clientId);

    // Contar ventas por cliente
    long countByClient(Client client);
    long countByClientId(Long clientId);

    // Ventas generadas por una factura específica
    @EntityGraph(attributePaths = {"product", "client"})
    List<Sale> findByInvoiceId(Long invoiceId);

    @EntityGraph(attributePaths = {"product", "client"})
    List<Sale> findTop10BySaleDateIsNotNullOrderBySaleDateDesc();

    // Los totales los calcula la base de datos, sin traer cada venta a memoria

    @Query("""
            select coalesce(sum(s.totalAmount), 0) as revenue,
                   coalesce(sum(s.costAmount), 0) as cost,
                   coalesce(sum(s.profitAmount), 0) as profit,
                   count(s) as salesCount,
                   coalesce(sum(s.quantity), 0) as itemsSold
            from Sale s
            """)
    Totals totals();

    @Query("""
            select coalesce(sum(s.totalAmount), 0) as revenue,
                   coalesce(sum(s.costAmount), 0) as cost,
                   coalesce(sum(s.profitAmount), 0) as profit,
                   count(s) as salesCount,
                   coalesce(sum(s.quantity), 0) as itemsSold
            from Sale s
            where s.saleDate >= :start and s.saleDate < :end
            """)
    Totals totalsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select cast(s.saleDate as LocalDate) as day,
                   coalesce(sum(s.totalAmount), 0) as revenue,
                   coalesce(sum(s.costAmount), 0) as cost,
                   coalesce(sum(s.profitAmount), 0) as profit,
                   count(s) as salesCount,
                   coalesce(sum(s.quantity), 0) as itemsSold
            from Sale s
            where s.saleDate >= :start and s.saleDate < :end
            group by cast(s.saleDate as LocalDate)
            """)
    List<DailyTotals> dailyTotals(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select p.id as productId,
                   p.productName as productName,
                   sum(s.quantity) as quantitySold,
                   coalesce(sum(s.totalAmount), 0) as revenue
            from Sale s join s.product p
            group by p.id, p.productName
            order by quantitySold desc, p.id
            """)
    List<TopProduct> topProducts(Limit limit);

    @Query("""
            select coalesce(nullif(p.category, ''), 'Uncategorized') as category,
                   coalesce(sum(s.totalAmount), 0) as revenue,
                   sum(s.quantity) as quantity
            from Sale s join s.product p
            group by coalesce(nullif(p.category, ''), 'Uncategorized')
            order by revenue desc
            """)
    List<CategoryTotals> categoryTotals();
}
