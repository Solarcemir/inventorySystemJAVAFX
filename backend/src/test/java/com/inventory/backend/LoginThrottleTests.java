package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inventory.backend.auth.LoginThrottle;
import com.inventory.backend.exception.ApiException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@IsolatedDatabaseTest
class LoginThrottleTests {

    @Autowired TestData data;
    @Autowired TestRestTemplate rest;

    @BeforeEach
    void reset() {
        data.clear();
        data.user("owner", "owner-pass", "ADMIN");
        data.user("vendor", "vendor-pass", "SELLER");
    }

    private ResponseEntity<String> login(String username, String password) {
        return rest.postForEntity("/api/login", Map.of("username", username, "password", password), String.class);
    }

    @Test
    void fiveWrongPasswordsLockTheUsernameEvenForTheRightPassword() {
        for (int i = 0; i < 5; i++) {
            assertThat(login("owner", "guess-" + i).getBody()).contains("\"success\":false");
        }

        ResponseEntity<String> locked = login("owner", "owner-pass");

        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(locked.getBody()).contains("Demasiados intentos");
    }

    @Test
    void anotherUserCanStillLogIn() {
        for (int i = 0; i < 5; i++) {
            login("owner", "guess-" + i);
        }

        assertThat(login("vendor", "vendor-pass").getBody()).contains("\"success\":true");
    }

    @Test
    void aCorrectLoginForgetsEarlierMistakes() {
        for (int i = 0; i < 4; i++) {
            login("owner", "guess-" + i);
        }
        assertThat(login("owner", "owner-pass").getBody()).contains("\"success\":true");

        for (int i = 0; i < 4; i++) {
            login("owner", "guess-" + i);
        }
        assertThat(login("owner", "owner-pass").getBody()).contains("\"success\":true");
    }

    @Test
    void unknownUsernamesAreTreatedLikeRealOnes() {
        for (int i = 0; i < 5; i++) {
            login("nobody", "guess-" + i);
        }

        assertThat(login("nobody", "anything").getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void theUsernameIsMatchedWithoutCaseOrSpaces() {
        for (int i = 0; i < 5; i++) {
            login(i % 2 == 0 ? "OWNER " : " owner", "guess-" + i);
        }

        assertThat(login("owner", "owner-pass").getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void theLockEnds() throws Exception {
        LoginThrottle throttle = new LoginThrottle(Duration.ofMillis(50));
        for (int i = 0; i < 5; i++) {
            throttle.recordFailure("owner");
        }
        assertThatThrownBy(() -> throttle.checkAllowed("owner")).isInstanceOf(ApiException.class);

        Thread.sleep(80);

        assertThatCode(() -> throttle.checkAllowed("owner")).doesNotThrowAnyException();
    }
}
