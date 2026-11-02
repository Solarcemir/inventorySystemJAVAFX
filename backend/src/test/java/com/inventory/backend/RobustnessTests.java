package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.inventory.backend.model.Client;
import com.inventory.backend.model.Invoice;
import com.inventory.backend.model.Product;
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

/** Wrong input, missing records and failures must never break the app or reveal how it works inside. */
@IsolatedDatabaseTest
class RobustnessTests {

    private static final List<String> INTERNALS = List.of(
            "Exception", "org.hibernate", "org.springframework", "at com.", "SQL", "select ", "insert ", "update ",
            "invoice_name", "constraint", "trace", "Users\\", "/Users/");

    @Autowired TestData data;
    @Autowired TestRestTemplate rest;
    @Autowired InvoiceService invoices;

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

    private String login(String username, String password) {
        return rest.postForEntity("/api/login", Map.of("username", username, "password", password), String.class)
                .getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> call(HttpMethod method, String path, String cookie, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private void assertRevealsNothing(ResponseEntity<String> response) {
        String body = response.getBody() != null ? response.getBody() : "";
        for (String internal : INTERNALS) {
            assertThat(body).as("response to reveal nothing about " + internal).doesNotContainIgnoringCase(internal);
        }
    }

    @Test
    void aTextTooLongForTheDatabaseIsRejectedWithoutDetails() {
        Invoice invoice = invoices.create(new Invoice());

        ResponseEntity<String> response = call(HttpMethod.PUT, "/api/invoices/" + invoice.getId() + "/settings", admin,
                Map.of("invoiceName", "x".repeat(300)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("demasiado largo");
        assertRevealsNothing(response);
    }

    @Test
    void malformedRequestsRevealNothing() {
        List<ResponseEntity<String>> responses = List.of(
                call(HttpMethod.GET, "/api/products/not-a-number", admin, null),
                call(HttpMethod.POST, "/api/clients", admin, "{ this is not json"),
                call(HttpMethod.POST, "/api/sales?productId=1", admin, null),
                call(HttpMethod.GET, "/api/does-not-exist", admin, null));

        assertThat(responses).extracting(r -> r.getStatusCode().is4xxClientError()).containsOnly(true);
        responses.forEach(this::assertRevealsNothing);
    }

    @Test
    void missingRecordsAnswerNotFoundWithAMessage() {
        assertThat(call(HttpMethod.GET, "/api/products/999999", admin, null).getBody()).contains("Producto no encontrado");
        assertThat(call(HttpMethod.GET, "/api/clients/999999", admin, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/api/invoices/999999", admin, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/api/installations/999999", admin, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.POST, "/api/sales?productId=999999&quantity=1", admin, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void everyResponseCarriesBrowserProtections() {
        for (ResponseEntity<String> response : List.of(
                call(HttpMethod.GET, "/api/health", admin, null),
                call(HttpMethod.GET, "/api/products", admin, null),
                call(HttpMethod.GET, "/api/products", null, null))) {
            assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
            assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(response.getHeaders().getFirst("Referrer-Policy")).isEqualTo("no-referrer");
        }
    }

    @Test
    void aDeletedProductCannotBeSoldOrInvoiced() {
        Product product = data.product("Old model", "100.00", "60.00", 10);
        Invoice invoice = invoices.create(new Invoice());
        call(HttpMethod.DELETE, "/api/products/" + product.getId(), admin, null);

        ResponseEntity<String> sale = call(HttpMethod.POST, "/api/sales?productId=" + product.getId() + "&quantity=1", seller, null);
        ResponseEntity<String> item = call(HttpMethod.POST, "/api/invoices/" + invoice.getId() + "/items?productId=" + product.getId() + "&quantity=1", seller, null);

        assertThat(sale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sale.getBody()).contains("eliminado");
        assertThat(item.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(data.stock(product)).isEqualTo(10);
    }

    @Test
    void aProductDeletedWhileInAnOpenInvoiceStopsTheFinalize() {
        Product product = data.product("Old model", "100.00", "60.00", 10);
        Invoice invoice = invoices.addItem(invoices.create(new Invoice()).getId(), product.getId(), 2);
        call(HttpMethod.DELETE, "/api/products/" + product.getId(), admin, null);

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/invoices/" + invoice.getId() + "/finalize", seller, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("quitalo de la factura");
        assertThat(data.stock(product)).isEqualTo(10);
        assertThat(invoices.getById(invoice.getId()).getStatus()).isEqualTo("OPEN");
    }

    @Test
    void aDeletedProductLeavesTheListsButKeepsItsSales() {
        Product product = data.product("Old model", "100.00", "60.00", 10);
        call(HttpMethod.POST, "/api/sales?productId=" + product.getId() + "&quantity=1", admin, null);

        call(HttpMethod.DELETE, "/api/products/" + product.getId(), admin, null);

        assertThat(call(HttpMethod.GET, "/api/products", admin, null).getBody()).doesNotContain("Old model");
        assertThat(call(HttpMethod.GET, "/api/products/count", admin, null).getBody()).isEqualTo("0");
        assertThat(call(HttpMethod.GET, "/api/sales", admin, null).getBody()).contains("Old model");
        assertThat(data.products.findById(product.getId())).isPresent();
    }

    @Test
    void aSaleToADeletedClientIsRejected() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        Client client = data.client("Gone", "Client");
        call(HttpMethod.DELETE, "/api/clients/" + client.getId(), admin, null);

        ResponseEntity<String> response = call(HttpMethod.POST,
                "/api/sales/with-client?productId=" + product.getId() + "&clientId=" + client.getId() + "&quantity=1", seller, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(data.stock(product)).isEqualTo(10);
    }

    @Test
    void anEmptyInvoiceCannotBeFinalized() {
        Invoice invoice = invoices.create(new Invoice());

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/invoices/" + invoice.getId() + "/finalize", seller, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(invoices.getById(invoice.getId()).getStatus()).isEqualTo("OPEN");
    }

    @Test
    void aFinalizedInvoiceKeepsItsLines() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        Invoice invoice = invoices.addItem(invoices.create(new Invoice()).getId(), product.getId(), 1);
        invoices.finalize(invoice.getId());
        String path = "/api/invoices/" + invoice.getId();

        assertThat(call(HttpMethod.POST, path + "/items?productId=" + product.getId() + "&quantity=1", admin, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(call(HttpMethod.DELETE, path + "/items/" + invoice.getItems().get(0).getId(), admin, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(invoices.getById(invoice.getId()).getItems()).hasSize(1);
    }

    @Test
    void installationsOnlyAcceptKnownStatuses() {
        Map<String, Object> body = new HashMap<>();
        body.put("serviceName", "Radio");
        body.put("installerName", "Tech");
        body.put("status", "DONE");

        assertThat(call(HttpMethod.POST, "/api/installations", seller, body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        body.remove("status");
        ResponseEntity<String> created = call(HttpMethod.POST, "/api/installations", seller, body);
        assertThat(created.getBody()).contains("\"status\":\"PENDING\"", "\"serviceDate\":\"");
    }

    @Test
    void anInstallationEditMustKeepAStatus() {
        Long id = data.installation("Radio", "50.00", "PENDING", LocalDateTime.now()).getId();
        Map<String, Object> body = new HashMap<>();
        body.put("serviceName", "Radio");
        body.put("price", 50);

        assertThat(call(HttpMethod.PUT, "/api/installations/" + id, admin, body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(data.installations.findById(id).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void calendarNotesAreCheckedBeforeSaving() {
        assertThat(call(HttpMethod.POST, "/api/calendar-notes", seller, Map.of("dateKey", "2026-02-30", "text", "Visit")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "/api/calendar-notes", seller, Map.of("dateKey", "tomorrow", "text", "Visit")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "/api/calendar-notes", seller, Map.of("dateKey", "2026-03-01", "text", "   ")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.POST, "/api/calendar-notes", seller, Map.of("dateKey", "2026-03-01", "text", "x".repeat(256))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(data.notes.count()).isZero();

        ResponseEntity<String> created = call(HttpMethod.POST, "/api/calendar-notes", seller, Map.of("dateKey", "2026-03-01", "text", "  Hilux 2025, 9am  "));
        assertThat(created.getBody()).contains("\"text\":\"Hilux 2025, 9am\"");
        Long id = data.notes.findAll().get(0).getId();
        assertThat(call(HttpMethod.DELETE, "/api/calendar-notes/" + id, seller, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(data.notes.count()).isZero();
    }

    @Test
    void theMostLoyalClientIsActiveAndCountsMissingTotalsAsZero() {
        Client noPurchases = data.client("No", "Purchases");
        noPurchases.setSpentAmount(null);
        data.clients.save(noPurchases);
        Client gone = data.client("Gone", "Big Spender");
        gone.setSpentAmount(new java.math.BigDecimal("9999"));
        gone.setDeleted(true);
        data.clients.save(gone);
        Client loyal = data.client("Loyal", "Client");
        loyal.setSpentAmount(new java.math.BigDecimal("50"));
        data.clients.save(loyal);

        assertThat(call(HttpMethod.GET, "/api/clients/most-loyal", admin, null).getBody()).contains("\"firstName\":\"Loyal\"");
    }

    @Test
    void everyChangeIsInTheActivityLog() {
        Product product = data.product("Speaker", "100.00", "60.00", 10);
        Map<String, Object> edited = new HashMap<>();
        edited.put("productName", "Speaker v2");
        edited.put("price", 120);
        edited.put("costPrice", 60);
        edited.put("productQuantity", 10);
        call(HttpMethod.PUT, "/api/products/" + product.getId(), admin, edited);
        call(HttpMethod.DELETE, "/api/products/" + product.getId(), admin, null);
        call(HttpMethod.POST, "/api/clients", seller, Map.of("firstName", "Ana", "lastName", "Gomez"));
        Long clientId = data.clients.findAll().get(0).getId();
        call(HttpMethod.PUT, "/api/clients/" + clientId, admin, Map.of("firstName", "Ana", "lastName", "Perez"));
        call(HttpMethod.DELETE, "/api/clients/" + clientId, admin, null);

        for (String action : List.of("EDIT_PRODUCT", "DELETE_PRODUCT", "ADD_CLIENT", "EDIT_CLIENT", "DELETE_CLIENT")) {
            assertThat(data.logs.findByActionOrderByTimestampDesc(action)).as(action).hasSize(1);
        }
        assertThat(data.logs.findByActionOrderByTimestampDesc("ADD_CLIENT").get(0).getUsername()).isEqualTo("vendor");
    }

    @Test
    void aVeryLongProductNameStillFitsInTheActivityLog() {
        Map<String, Object> product = new HashMap<>();
        product.put("productName", "y".repeat(250));
        product.put("price", 1);
        product.put("productQuantity", 1);

        assertThat(call(HttpMethod.POST, "/api/products", admin, product).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.logs.findByActionOrderByTimestampDesc("ADD_PRODUCT").get(0).getDescription()).hasSizeLessThanOrEqualTo(500);
    }
}
