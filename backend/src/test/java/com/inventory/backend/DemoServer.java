package com.inventory.backend;

import com.inventory.backend.model.Client;
import com.inventory.backend.model.Installation;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.model.Product;
import com.inventory.backend.services.InstallationServiceBean;
import com.inventory.backend.services.InvoiceService;
import com.inventory.backend.services.SaleService;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs the whole app on a throwaway in-memory database with sample data, to try
 * changes without touching real data. Nothing is saved when it stops.
 *
 *   mvn spring-boot:test-run -Dspring-boot.run.main-class=com.inventory.backend.DemoServer
 *
 * Logins: admin / admin and seller / seller.
 */
public class DemoServer {

    public static void main(String[] args) {
        // System properties outrank config/application.properties
        System.setProperty("spring.config.location", "classpath:/application.properties");
        System.setProperty("spring.datasource.url",
                "jdbc:h2:mem:demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.H2Dialect");

        // Keeps the demo away from the real Documents\CFT-Electronic\images folder
        System.setProperty("user.home", Paths.get("target", "demo-home").toAbsolutePath().toString());

        SpringApplication.run(new Class<?>[] {BackendApplication.class, SampleData.class}, args);
    }

    @Configuration
    static class SampleData {

        @Bean
        CommandLineRunner seed(TestData data, SaleService sales, InvoiceService invoices,
                               InstallationServiceBean installations) {
            return args -> {
                data.user("admin", "admin", "ADMIN");
                data.user("seller", "seller", "SELLER");

                Product radio = data.product("Pioneer Radio", "120.00", "75.00", 12);
                Product speakers = data.product("JBL Speakers", "85.50", "50.00", 3);
                Product camera = data.product("Reverse Camera", "45.00", "20.00", 8);
                Client ana = data.client("Ana", "Gomez");
                data.client("Carlos", "Rivas");

                sales.createSale(radio.getId(), 1);
                sales.createSaleWithClient(camera.getId(), ana.getId(), 2);

                Invoice invoice = invoices.create(new Invoice());
                invoices.addItem(invoice.getId(), radio.getId(), 2);
                invoices.addItem(invoice.getId(), speakers.getId(), 1);
                invoices.updateSettings(invoice.getId(), "Order for Ana", "Ana Gomez", new BigDecimal("10"), "FAC-001", "Hilux 2025");
                invoices.finalize(invoice.getId());

                data.installation("Radio installation", "60.00", "COMPLETED", LocalDateTime.now());
                Installation pending = data.installation("Window tint", "160.00", "PENDING", LocalDateTime.now());
                pending.setClientName("Carlos Rivas");
                pending.setCarModel("ix35 2025");
                data.installations.save(pending);
            };
        }
    }
}
