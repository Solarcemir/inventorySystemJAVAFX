package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.backend.model.Client;
import com.inventory.backend.model.Product;
import com.inventory.backend.services.IdempotencyService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@IsolatedDatabaseTest
class IdempotencyTests {

    @Autowired TestData data;
    @Autowired TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();
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

    private ResponseEntity<String> post(String path, String cookie, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        if (key != null) {
            headers.add("Idempotency-Key", key);
        }
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> sell(Product product, int quantity, String cookie, String key) {
        return post("/api/sales?productId=" + product.getId() + "&quantity=" + quantity, cookie, key);
    }

    private long saleId(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody()).get("id").asLong();
    }

    @Test
    void aRepeatedRequestSellsOnceAndReturnsTheSameSale() throws Exception {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        String key = UUID.randomUUID().toString();

        ResponseEntity<String> first = sell(speaker, 2, admin, key);
        ResponseEntity<String> repeat = sell(speaker, 2, admin, key);

        assertThat(repeat.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saleId(repeat)).isEqualTo(saleId(first));
        assertThat(data.sales.count()).isEqualTo(1);
        assertThat(data.stock(speaker)).isEqualTo(8);
        assertThat(data.logs.findByActionOrderByTimestampDesc("SALE")).hasSize(1);
    }

    @Test
    void aRepeatedSaleWithAClientCountsOnceTowardsTheirTotal() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Client ana = data.client("Ana", "Gomez");
        String path = "/api/sales/with-client?productId=" + speaker.getId() + "&clientId=" + ana.getId() + "&quantity=1";
        String key = UUID.randomUUID().toString();

        post(path, admin, key);
        post(path, admin, key);

        assertThat(data.sales.count()).isEqualTo(1);
        assertThat(data.clients.findById(ana.getId()).orElseThrow().getSpentAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void differentKeysAreDifferentSales() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);

        sell(speaker, 1, admin, UUID.randomUUID().toString());
        sell(speaker, 1, admin, UUID.randomUUID().toString());

        assertThat(data.sales.count()).isEqualTo(2);
        assertThat(data.stock(speaker)).isEqualTo(8);
    }

    @Test
    void withoutAKeySalesWorkAsBefore() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);

        assertThat(sell(speaker, 1, admin, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sell(speaker, 1, admin, "   ").getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(data.sales.count()).isEqualTo(2);
    }

    @Test
    void simultaneousRepeatsStillSellOnce() throws Exception {
        Product speaker = data.product("Speaker", "100.00", "60.00", 50);
        String key = UUID.randomUUID().toString();
        int clicks = 12;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(clicks);
        List<Future<ResponseEntity<String>>> responses = new ArrayList<>();

        for (int i = 0; i < clicks; i++) {
            responses.add(pool.submit(() -> {
                start.await();
                return sell(speaker, 1, admin, key);
            }));
        }
        start.countDown();

        List<Long> ids = new ArrayList<>();
        for (Future<ResponseEntity<String>> response : responses) {
            ids.add(saleId(response.get(30, TimeUnit.SECONDS)));
        }
        pool.shutdown();

        assertThat(Set.copyOf(ids)).hasSize(1);
        assertThat(data.sales.count()).isEqualTo(1);
        assertThat(data.stock(speaker)).isEqualTo(49);
    }

    @Test
    void aFailedAttemptIsForgottenSoItCanBeRetried() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 1);
        String key = UUID.randomUUID().toString();

        ResponseEntity<String> refused = sell(speaker, 5, admin, key);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody()).contains("Stock insuficiente");

        speaker.setProductQuantity(10);
        data.products.save(speaker);

        assertThat(sell(speaker, 5, admin, key).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.stock(speaker)).isEqualTo(5);
    }

    @Test
    void theSameKeyFromAnotherUserIsAnotherSale() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        String key = UUID.randomUUID().toString();

        sell(speaker, 1, admin, key);
        sell(speaker, 1, seller, key);

        assertThat(data.sales.count()).isEqualTo(2);
    }

    @Test
    void theSameKeyForAnotherProductOrQuantityIsAnotherSale() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        Product radio = data.product("Radio", "50.00", "20.00", 10);
        String key = UUID.randomUUID().toString();

        sell(speaker, 1, admin, key);
        sell(radio, 1, admin, key);
        sell(speaker, 2, admin, key);

        assertThat(data.sales.count()).isEqualTo(3);
    }

    @Test
    void aRepeatedSaleStillHidesCostsFromSellers() {
        Product speaker = data.product("Speaker", "100.00", "60.00", 10);
        String key = UUID.randomUUID().toString();

        sell(speaker, 1, seller, key);
        String repeat = sell(speaker, 1, seller, key).getBody();

        assertThat(repeat).contains("\"quantity\":1").doesNotContain("costPrice", "costAmount", "profitAmount", "totalAmount");
    }

    @Test
    void keysExpire() throws Exception {
        IdempotencyService shortLived = new IdempotencyService(Duration.ofMillis(5));
        AtomicInteger runs = new AtomicInteger();

        shortLived.runOnce("key", runs::incrementAndGet);
        shortLived.runOnce("key", runs::incrementAndGet);
        assertThat(runs.get()).isEqualTo(1);

        Thread.sleep(20);
        shortLived.runOnce("key", runs::incrementAndGet);
        assertThat(runs.get()).isEqualTo(2);
    }

    @Test
    void aFailureReachesEveryoneWaitingForTheFirstAttempt() throws Exception {
        IdempotencyService service = new IdempotencyService();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Object> first = pool.submit(() -> service.runOnce("key", () -> {
            firstStarted.countDown();
            await(release);
            throw com.inventory.backend.exception.ApiException.conflict("Stock insuficiente");
        }));
        firstStarted.await();
        Future<Object> repeat = pool.submit(() -> service.runOnce("key", () -> "must not run"));
        Thread.sleep(50);
        release.countDown();

        List<String> messages = new ArrayList<>();
        for (Future<Object> attempt : List.of(first, repeat)) {
            try {
                attempt.get(10, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException e) {
                messages.add(e.getCause().getMessage());
            }
        }
        pool.shutdown();

        assertThat(messages.stream().collect(Collectors.toSet())).containsExactly("Stock insuficiente");
        assertThat(messages).hasSize(2);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
