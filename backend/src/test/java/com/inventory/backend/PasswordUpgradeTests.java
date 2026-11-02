package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.inventory.backend.model.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@IsolatedDatabaseTest
class PasswordUpgradeTests {

    @Autowired TestData data;
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void reset() {
        data.clear();
    }

    private ResponseEntity<String> login(String username, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);
        return rest.postForEntity("/api/login", credentials, String.class);
    }

    private boolean loggedIn(ResponseEntity<String> response) {
        return response.getBody().contains("\"success\":true")
                && response.getHeaders().getFirst(HttpHeaders.SET_COOKIE) != null;
    }

    private String stored(String username) {
        return data.users.findByUsername(username).getPassword();
    }

    @Test
    void aPlainTextPasswordIsHashedAtTheFirstCorrectLogin() {
        data.user("owner", "secret-1", "ADMIN");

        assertThat(loggedIn(login("owner", "secret-1"))).isTrue();

        assertThat(stored("owner")).isNotEqualTo("secret-1").startsWith("$2");
        assertThat(encoder.matches("secret-1", stored("owner"))).isTrue();
    }

    @Test
    void theSamePasswordKeepsWorkingAfterTheUpgrade() {
        data.user("owner", "secret-1", "SELLER");

        login("owner", "secret-1");
        ResponseEntity<String> second = login("owner", "secret-1");

        assertThat(loggedIn(second)).isTrue();
        assertThat(second.getBody()).contains("\"username\":\"owner\"", "\"role\":\"SELLER\"");
    }

    @Test
    void aHashIsNotHashedAgain() {
        data.user("owner", "secret-1", "ADMIN");
        login("owner", "secret-1");
        String firstHash = stored("owner");

        login("owner", "secret-1");

        assertThat(stored("owner")).isEqualTo(firstHash);
    }

    @Test
    void aWrongPasswordChangesNothing() {
        data.user("owner", "secret-1", "ADMIN");

        assertThat(loggedIn(login("owner", "wrong"))).isFalse();

        assertThat(stored("owner")).isEqualTo("secret-1");
    }

    @Test
    void aWrongPasswordIsRejectedAfterTheUpgrade() {
        data.user("owner", "secret-1", "ADMIN");
        login("owner", "secret-1");

        assertThat(loggedIn(login("owner", "wrong"))).isFalse();
    }

    @Test
    void theStoredHashCannotBeUsedAsAPassword() {
        data.user("owner", "secret-1", "ADMIN");
        login("owner", "secret-1");

        assertThat(loggedIn(login("owner", stored("owner")))).isFalse();
    }

    @Test
    void oneUsersLoginLeavesOtherUsersUntouched() {
        data.user("owner", "secret-1", "ADMIN");
        data.user("vendor", "secret-2", "SELLER");

        login("owner", "secret-1");

        assertThat(stored("vendor")).isEqualTo("secret-2");
        assertThat(loggedIn(login("vendor", "secret-2"))).isTrue();
    }

    // How to reset a forgotten password: write a new one in the users table
    @Test
    void aNewPlainTextPasswordWrittenInTheDatabaseWorksAndGetsHashed() {
        data.user("owner", "secret-1", "ADMIN");
        login("owner", "secret-1");

        jdbc.update("update users set password = ? where username = ?", "brand-new", "owner");

        assertThat(loggedIn(login("owner", "secret-1"))).isFalse();
        assertThat(loggedIn(login("owner", "brand-new"))).isTrue();
        assertThat(stored("owner")).startsWith("$2");
    }

    @Test
    void loginStillWorksWhenTheHashCannotBeStored() {
        data.user("owner", "secret-1", "ADMIN");
        jdbc.execute("alter table users alter column password set data type varchar(20)");
        try {
            assertThat(loggedIn(login("owner", "secret-1"))).isTrue();
            assertThat(stored("owner")).isEqualTo("secret-1");
            assertThat(data.logs.findByActionOrderByTimestampDesc("LOGIN")).hasSize(1);
        } finally {
            jdbc.execute("alter table users alter column password set data type varchar(255)");
        }
    }

    @Test
    void aPasswordTooLongForBcryptStillLogsInAsBefore() {
        String longPassword = "x".repeat(100);
        data.user("owner", longPassword, "ADMIN");

        assertThat(loggedIn(login("owner", longPassword))).isTrue();

        assertThat(stored("owner")).isEqualTo(longPassword);
    }

    @Test
    void aMissingPasswordIsRejected() {
        data.user("owner", "secret-1", "ADMIN");

        assertThat(loggedIn(login("owner", null))).isFalse();
    }

    @Test
    void anUnknownUserIsRejected() {
        assertThat(loggedIn(login("nobody", "secret-1"))).isFalse();
    }

    @Test
    void theLoginResponseNeverIncludesThePassword() {
        User user = data.user("owner", "secret-1", "ADMIN");

        String body = login("owner", "secret-1").getBody();

        assertThat(body).doesNotContain("secret-1", "password", "$2");
        assertThat(body).contains("\"id\":" + user.getId());
    }
}
