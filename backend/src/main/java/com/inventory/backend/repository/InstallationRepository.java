package com.inventory.backend.repository;

import com.inventory.backend.model.Installation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InstallationRepository extends JpaRepository<Installation, Long> {

    interface RevenueTotals {
        BigDecimal getRevenue();
        long getServices();
    }

    interface DailyRevenue {
        LocalDate getDay();
        BigDecimal getRevenue();
    }

    List<Installation> findByInstallerName(String installerName);
    List<Installation> findByStatus(String status);
    List<Installation> findAllByOrderByServiceDateDesc();
    List<Installation> findByInvoiceId(Long invoiceId);

    @Query("""
            select coalesce(sum(i.price), 0) as revenue, count(i) as services
            from Installation i
            where i.status = 'COMPLETED'
            """)
    RevenueTotals completedTotals();

    @Query("""
            select coalesce(sum(i.price), 0) as revenue, count(i) as services
            from Installation i
            where i.status = 'COMPLETED' and i.serviceDate >= :start and i.serviceDate < :end
            """)
    RevenueTotals completedTotalsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select cast(i.serviceDate as LocalDate) as day, coalesce(sum(i.price), 0) as revenue
            from Installation i
            where i.status = 'COMPLETED' and i.serviceDate >= :start and i.serviceDate < :end
            group by cast(i.serviceDate as LocalDate)
            """)
    List<DailyRevenue> completedRevenueByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
