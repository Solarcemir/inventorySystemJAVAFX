package com.inventory.backend;

import com.inventory.backend.auth.LoginThrottle;
import com.inventory.backend.model.Client;
import com.inventory.backend.model.Installation;
import com.inventory.backend.model.Product;
import com.inventory.backend.model.User;
import com.inventory.backend.repository.ActivityLogRepository;
import com.inventory.backend.repository.CalendarNoteRepository;
import com.inventory.backend.repository.ClientRepository;
import com.inventory.backend.repository.InstallationRepository;
import com.inventory.backend.repository.InvoiceRepository;
import com.inventory.backend.repository.ProductRepository;
import com.inventory.backend.repository.SaleRepository;
import com.inventory.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Builds and clears records for tests. */
@Component
class TestData {

    @Autowired ProductRepository products;
    @Autowired ClientRepository clients;
    @Autowired SaleRepository sales;
    @Autowired InvoiceRepository invoices;
    @Autowired InstallationRepository installations;
    @Autowired ActivityLogRepository logs;
    @Autowired CalendarNoteRepository notes;
    @Autowired UserRepository users;
    @Autowired LoginThrottle loginThrottle;

    void clear() {
        loginThrottle.reset();
        sales.deleteAll();
        invoices.deleteAll();
        installations.deleteAll();
        logs.deleteAll();
        notes.deleteAll();
        products.deleteAll();
        clients.deleteAll();
        users.deleteAll();
    }

    Product product(String name, String price, String cost, int quantity) {
        Product product = new Product();
        product.setProductName(name);
        product.setProvider("Vendor");
        product.setCategory("Audio");
        product.setPrice(new BigDecimal(price));
        product.setCostPrice(new BigDecimal(cost));
        product.setProductQuantity(quantity);
        product.setDeleted(false);
        return products.save(product);
    }

    Client client(String firstName, String lastName) {
        return clients.save(new Client(firstName, lastName, "", ""));
    }

    Installation installation(String service, String price, String status, LocalDateTime date) {
        Installation installation = new Installation();
        installation.setServiceName(service);
        installation.setInstallerName("Tech");
        installation.setClientName("Walk-in");
        installation.setPrice(new BigDecimal(price));
        installation.setStatus(status);
        installation.setServiceDate(date);
        return installations.save(installation);
    }

    User user(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return users.save(user);
    }

    int stock(Product product) {
        return products.findById(product.getId()).orElseThrow().getProductQuantity();
    }
}
