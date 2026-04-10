package com.team22.eventticketing.user;

import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class UserPreferencesIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private static final String BASE = "http://localhost:8080/api/users";

    // ─── a) Create user with initial preferences ──────────────────────────────────

    @Test
    void updatePreferences_mergesAndReturns404ForMissing() {

        // a) Create user
        User user = new User(
                "Test User",
                "test-" + UUID.randomUUID() + "@example.com",
                "password123",
                "01" + (int)(Math.random() * 900000000 + 100000000),
                Role.ATTENDEE
        );
        user.setPreferences(Map.of("language", "en", "favoriteCategory", "CONCERT"));

        ResponseEntity<User> createResp = rest.postForEntity(BASE, user, User.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = createResp.getBody().getId();

        // b) PUT /api/users/{id}/preferences with partial update
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> patchReq = new HttpEntity<>(
                Map.of("favoriteCategory", "SPORTS", "ticketTier", "VIP"),
                headers
        );

        ResponseEntity<Map<String, Object>> patchResp = rest.exchange(
                BASE + "/" + id + "/preferences",
                HttpMethod.PUT,
                patchReq,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // c) Verify merge: language kept, favoriteCategory updated, ticketTier added
        assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> prefs = (Map<String, Object>) patchResp.getBody().get("preferences");
        assertThat(prefs)
                .containsEntry("language", "en")
                .containsEntry("favoriteCategory", "SPORTS")
                .containsEntry("ticketTier", "VIP");

        // d) PUT /api/users/999/preferences → 404
        ResponseEntity<Void> notFound = rest.exchange(
                BASE + "/999/preferences",
                HttpMethod.PUT,
                patchReq,
                Void.class
        );
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
