package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.inventory.backend.model.ActivityLog;
import com.inventory.backend.model.Client;
import com.inventory.backend.model.Installation;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.model.Product;
import com.inventory.backend.services.InstallationServiceBean;
import com.inventory.backend.services.InvoiceService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@IsolatedDatabaseTest
class ApiSecurityTests {

    @Autowired TestData data;
    @Autowired TestRestTemplate rest;
    @Autowired InvoiceService invoices;
    @Autowired InstallationServiceBean installationService;

    private String admin;
    private String seller;

    @BeforeEach
    void reset() {
        data.clear();
        data.user("owner", "owner-pass", "ADMIN");
        data.user("vendor", "vendor-pass", "SELLER");
        admin = login("owner", "owner-pass");
        seller = login("vendor", "vendor-pass");
    }

    private ResponseEntity<String> loginResponse(String username, String password) {
        return rest.postForEntity("/api/login", Map.of("username", username, "password", password), String.class);
    }

    private String login(String username, String password) {
        return loginResponse(username, password).getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> call(HttpMethod method, String path, String cookie, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String path, String cookie) {
        return call(HttpMethod.GET, path, cookie, null);
    }

    @Test
    void visitorsWithoutASessionGetNothing() {
        for (String path : List.of("/api/products", "/api/clients", "/api/sales", "/api/sales/count",
                "/api/invoices", "/api/installations", "/api/calendar-notes", "/api/analytics/summary",
                "/api/logs", "/api/images/list", "/api/images/serve/photo.jpg")) {
            assertThat(get(path, null).getStatusCode()).as(path).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(call(HttpMethod.POST, "/api/sales?productId=1&quantity=1", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthStaysOpenForTheLauncher() {
        assertThat(get("/api/health", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void wrongPasswordOpensNoSession() {
        ResponseEntity<String> response = loginResponse("owner", "wrong");

        assertThat(response.getBody()).contains("\"success\":false");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void loginStillReturnsTheSameFields() {
        assertThat(loginResponse("vendor", "vendor-pass").getBody())
                .contains("\"success\":true", "\"username\":\"vendor\"", "\"role\":\"SELLER\"", "\"id\":");
    }

    @Test
    void sessionCookieIsHiddenFromScriptsAndOtherSites() {
        String cookie = loginResponse("owner", "owner-pass").getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertThat(cookie).contains("HttpOnly").containsIgnoringCase("SameSite=Strict");
    }

    @Test
    void logoutEndsTheSession() {
        assertThat(get("/api/products", admin).getStatusCode()).isEqualTo(HttpStatus.OK);

        call(HttpMethod.POST, "/api/logout", admin, null);

        assertThat(get("/api/products", admin).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sellersCannotUseAdminEndpoints() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        Client client = data.client("Ana", "Gomez");

        assertThat(get("/api/analytics/summary", seller).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/logs", seller).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/images/folder", seller).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.POST, "/api/products", seller, product).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/api/products/" + product.getId(), seller, product).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/api/products/" + product.getId(), seller, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.PUT, "/api/clients/" + client.getId(), seller, client).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/api/clients/" + client.getId(), seller, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, "/api/sales/1", seller, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sellersKeepTheirDailyWork() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);

        assertThat(call(HttpMethod.POST, "/api/sales?productId=" + product.getId() + "&quantity=1", seller, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "/api/clients", seller, Map.of("firstName", "Ana", "lastName", "Gomez")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "/api/invoices", seller, Map.of("invoiceName", "Order")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, "/api/calendar-notes", seller, Map.of("dateKey", "2026-01-01", "text", "Note")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/sales", seller).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/images/list", seller).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sellersDoNotReceiveCostsOrEarnings() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        call(HttpMethod.POST, "/api/sales?productId=" + product.getId() + "&quantity=1", seller, null);
        data.client("Ana", "Gomez");

        assertThat(get("/api/products", seller).getBody()).contains("Speaker", "\"price\"").doesNotContain("costPrice");
        assertThat(get("/api/clients", seller).getBody()).contains("Ana").doesNotContain("spentAmount");
        assertThat(get("/api/sales", seller).getBody()).contains("saleDate", "\"quantity\"")
                .doesNotContain("costPrice", "costAmount", "profitAmount", "totalAmount");
    }

    @Test
    void adminsReceiveTheFullRecords() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        call(HttpMethod.POST, "/api/sales?productId=" + product.getId() + "&quantity=1", admin, null);
        data.client("Ana", "Gomez");

        assertThat(get("/api/products", admin).getBody()).contains("costPrice");
        assertThat(get("/api/clients", admin).getBody()).contains("spentAmount");
        assertThat(get("/api/sales", admin).getBody()).contains("costAmount", "profitAmount", "totalAmount");
    }

    @Test
    void activityIsRecordedByTheServerUnderTheLoggedInUser() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);

        call(HttpMethod.POST, "/api/sales?productId=" + product.getId() + "&quantity=2", seller, null);

        List<ActivityLog> entries = data.logs.findByActionOrderByTimestampDesc("SALE");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getUsername()).isEqualTo("vendor");
        assertThat(entries.get(0).getDescription()).isEqualTo("Sold 2x \"Speaker\" for $200.00");
    }

    @Test
    void loginAndInstallationsAppearInTheActivityLog() {
        call(HttpMethod.POST, "/api/installations", seller, Map.of("serviceName", "Radio", "installerName", "Tech", "price", 10));

        assertThat(data.logs.findByActionOrderByTimestampDesc("LOGIN")).hasSize(2);
        assertThat(data.logs.findByActionOrderByTimestampDesc("INSTALLATION")).hasSize(1);
    }

    @Test
    void activityLogCannotBeForgedFromTheBrowser() {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/logs", admin,
                Map.of("action", "SALE", "description", "Fake", "username", "owner"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void theInvoiceSellerComesFromTheSession() {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/invoices", seller,
                Map.of("invoiceName", "Order", "sellerUsername", "someone-else"));

        assertThat(response.getBody()).contains("\"sellerUsername\":\"vendor\"");
    }

    @Test
    void aNewInvoiceIgnoresItemsTotalsAndStatusFromTheBrowser() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        Map<String, Object> forged = new HashMap<>();
        forged.put("invoiceName", "Order");
        forged.put("clientName", "Ana");
        forged.put("status", "FINALIZED");
        forged.put("total", 1);
        forged.put("subtotal", 1);
        forged.put("taxPercent", 0);
        forged.put("discountPercent", 90);
        forged.put("finalizedAt", "2020-01-01T00:00:00");
        forged.put("items", List.of(Map.of("productId", product.getId(), "productName", "Free", "quantity", 5, "unitPrice", 0, "lineTotal", -500)));

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/invoices", seller, forged);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"invoiceName\":\"Order\"", "\"clientName\":\"Ana\"", "\"status\":\"OPEN\"",
                "\"items\":[]", "\"total\":0", "\"taxPercent\":7", "\"discountPercent\":0", "\"finalizedAt\":null");
    }

    private Invoice openInvoiceWith(Product product) {
        Invoice invoice = invoices.create(new Invoice());
        return invoices.addItem(invoice.getId(), product.getId(), 2);
    }

    private ResponseEntity<String> changeSettings(Long invoiceId, String cookie, Map<String, String> settings) {
        return call(HttpMethod.PUT, "/api/invoices/" + invoiceId + "/settings", cookie, settings);
    }

    private Map<String, Object> installationBody(Object price, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("serviceName", "Radio");
        body.put("installerName", "Tech");
        body.put("price", price);
        body.put("status", status);
        return body;
    }

    @Test
    void sellersFinalizeInvoicesButOnlyAdminsCanUndoThem() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        Invoice invoice = openInvoiceWith(product);
        String path = "/api/invoices/" + invoice.getId();

        assertThat(call(HttpMethod.POST, path + "/finalize", seller, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, path + "/reopen", seller, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.DELETE, path, seller, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(data.stock(product)).isEqualTo(8);

        assertThat(call(HttpMethod.POST, path + "/reopen", admin, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.stock(product)).isEqualTo(10);
        assertThat(call(HttpMethod.DELETE, path, admin, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sellersCanWorkOnAnOpenInvoiceAndCancelIt() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        Invoice invoice = openInvoiceWith(product);
        String path = "/api/invoices/" + invoice.getId();

        assertThat(changeSettings(invoice.getId(), seller, Map.of("clientName", "Ana", "carModel", "Hilux")).getBody())
                .contains("\"clientName\":\"Ana\"", "\"carModel\":\"Hilux\"");
        assertThat(call(HttpMethod.POST, path + "/items?productId=" + product.getId() + "&quantity=1", seller, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.DELETE, path + "/items/" + invoice.getItems().get(0).getId(), seller, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.POST, path + "/cancel", seller, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void onlyAdminsCanChangeADiscount() {
        Invoice invoice = openInvoiceWith(data.product("Speaker", "100.00", "60.00", 10));

        ResponseEntity<String> refused = changeSettings(invoice.getId(), seller, Map.of("discountPercent", "50"));
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).contains("descuento");
        assertThat(invoices.getById(invoice.getId()).getDiscountPercent()).isEqualByComparingTo("0");

        // The settings screen resends the current discount when a seller saves other fields
        assertThat(changeSettings(invoice.getId(), seller, Map.of("discountPercent", "0", "clientName", "Ana")).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(changeSettings(invoice.getId(), admin, Map.of("discountPercent", "50")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(invoices.getById(invoice.getId()).getTotal()).isEqualByComparingTo("107.00");
    }

    @Test
    void sellersCannotEditAClosedInvoice() {
        Invoice invoice = openInvoiceWith(data.product("Speaker", "100.00", "60.00", 10));
        invoices.finalize(invoice.getId());

        assertThat(changeSettings(invoice.getId(), seller, Map.of("clientName", "Someone else")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(changeSettings(invoice.getId(), admin, Map.of("clientName", "Corrected")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void sellersCreateAndAdvanceInstallationsButCannotRemoveThem() {
        ResponseEntity<String> created = call(HttpMethod.POST, "/api/installations", seller, installationBody(50, "PENDING"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long id = data.installations.findAll().get(0).getId();
        String path = "/api/installations/" + id;

        assertThat(call(HttpMethod.PUT, path, seller, installationBody(50, "COMPLETED")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.installations.findById(id).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        assertThat(call(HttpMethod.DELETE, path, seller, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(call(HttpMethod.DELETE, path, admin, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.installations.count()).isZero();
    }

    @Test
    void sellersCannotChangeAnInstallationsPrice() {
        Long id = data.installation("Radio", "50.00", "PENDING", LocalDateTime.now()).getId();
        String path = "/api/installations/" + id;

        ResponseEntity<String> refused = call(HttpMethod.PUT, path, seller, installationBody(1, "PENDING"));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).contains("precio");
        assertThat(data.installations.findById(id).orElseThrow().getPrice()).isEqualByComparingTo("50.00");

        assertThat(call(HttpMethod.PUT, path, admin, installationBody(75, "PENDING")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.installations.findById(id).orElseThrow().getPrice()).isEqualByComparingTo("75.00");
    }

    @Test
    void aCompletedInstallationIsClosedToSellers() {
        Long id = data.installation("Radio", "50.00", "COMPLETED", LocalDateTime.now()).getId();

        ResponseEntity<String> refused = call(HttpMethod.PUT, "/api/installations/" + id, seller, installationBody(50, "CANCELLED"));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(data.installations.findById(id).orElseThrow().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void aSellerEditingAnInstallationKeepsItsInvoiceLink() {
        Installation installation = data.installation("Radio", "50.00", "PENDING", LocalDateTime.now());
        Invoice invoice = installationService.createInvoice(installation.getId(), "vendor");
        Map<String, Object> body = installationBody(50, "PENDING");
        body.put("invoiceId", 999999);

        call(HttpMethod.PUT, "/api/installations/" + installation.getId(), seller, body);

        assertThat(data.installations.findById(installation.getId()).orElseThrow().getInvoiceId()).isEqualTo(invoice.getId());
    }

    @Test
    void searchTextIsTreatedAsDataNotAsAQuery() {
        data.user("x' or '1'='1", "pass", "ADMIN");
        data.product("Speaker", "100.00", "60.00", 10);

        assertThat(loginResponse("owner' --", "owner-pass").getBody()).contains("\"success\":false");
        assertThat(loginResponse("' or '1'='1", "' or '1'='1").getBody()).contains("\"success\":false");
        assertThat(get("/api/logs/user/' or '1'='1", admin).getBody()).isEqualTo("[]");
        assertThat(get("/api/invoices/seller/' or '1'='1", admin).getBody()).isEqualTo("[]");
        assertThat(data.products.count()).isEqualTo(1);
    }

    @Test
    void businessErrorsReachTheBrowserWithTheirMessage() {
        Product product = data.product("Speaker", "100.00", "60.00", 2);

        ResponseEntity<String> response = call(HttpMethod.POST,
                "/api/sales?productId=" + product.getId() + "&quantity=5", seller, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("\"message\":\"Stock insuficiente. Disponible: 2\"");
    }

    @Test
    void adminsCanEditClientsButNotTheirTotalSpent() {
        Client client = data.client("Ana", "Gomez");

        ResponseEntity<String> response = call(HttpMethod.PUT, "/api/clients/" + client.getId(), admin,
                Map.of("firstName", "Ana Maria", "lastName", "Gomez", "email", "ana@example.com", "phoneNumber", "123", "spentAmount", 9999));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Client saved = data.clients.findById(client.getId()).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Ana Maria");
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getSpentAmount()).isEqualByComparingTo("0");
    }

    @Test
    void creatingAClientCannotOverwriteAnExistingOne() {
        Client client = data.client("Ana", "Gomez");

        call(HttpMethod.POST, "/api/clients", seller,
                Map.of("id", client.getId(), "firstName", "New", "lastName", "Person", "spentAmount", 500));

        assertThat(data.clients.findById(client.getId()).orElseThrow().getFirstName()).isEqualTo("Ana");
        assertThat(data.clients.count()).isEqualTo(2);
    }

    @Test
    void negativeProductValuesAreRejected() {
        Map<String, Object> product = Map.of("productName", "Bad", "price", -1, "costPrice", 1, "productQuantity", 1);

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/products", admin, product);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(data.products.count()).isZero();
    }
}
