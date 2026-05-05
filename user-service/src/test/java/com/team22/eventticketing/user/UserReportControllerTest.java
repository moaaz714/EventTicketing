package com.team22.eventticketing.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserReportControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM bookings WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test-s1f6-%')");
        jdbcTemplate.execute("DELETE FROM users WHERE email LIKE 'test-s1f6-%'");
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        cleanUp();

        Long userA = createUser("User A", "test-s1f6-a@test.com");
        Long userB = createUser("User B", "test-s1f6-b@test.com");
        Long userC = createUser("User C", "test-s1f6-c@test.com");

        // User A: 2000 total
        insertCompletedBooking(userA, 1200.0, "2026-03-10");
        insertCompletedBooking(userA,  800.0, "2026-03-15");
        // User B: 5000 total
        insertCompletedBooking(userB, 5000.0, "2026-03-20");
        // User C: 800 total
        insertCompletedBooking(userC, 800.0, "2026-03-25");
    }

    // Scenario b: top 2 in March 2026 → User B first, User A second
    @Test
    void topAttendees_returnsTopNOrderedBySpending() throws Exception {
        mockMvc.perform(get("/api/users/reports/top-attendees")
                        .param("startDate", "2026-03-01")
                        .param("endDate",   "2026-03-31")
                        .param("limit",     "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("User B")))
                .andExpect(jsonPath("$[0].totalSpent", is(5000.0)))
                .andExpect(jsonPath("$[1].name", is("User A")))
                .andExpect(jsonPath("$[1].totalSpent", is(2000.0)));
    }

    // Scenario c: no bookings in range → empty list
    @Test
    void topAttendees_emptyWhenNoBookingsInRange() throws Exception {
        mockMvc.perform(get("/api/users/reports/top-attendees")
                        .param("startDate", "2025-01-01")
                        .param("endDate",   "2025-01-31")
                        .param("limit",     "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // Scenario d: startDate after endDate → 400
    @Test
    void topAttendees_returns400WhenStartAfterEnd() throws Exception {
        mockMvc.perform(get("/api/users/reports/top-attendees")
                        .param("startDate", "2026-03-31")
                        .param("endDate",   "2026-03-01")
                        .param("limit",     "10"))
                .andExpect(status().isBadRequest());
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private Long createUser(String name, String email) {
        String phone = String.valueOf(Math.abs(email.hashCode() % 9_000_000_000L) + 1_000_000_000L);
        jdbcTemplate.update(
            "INSERT INTO users (name, email, password, phone, role, status, created_at) VALUES (?, ?, 'password', ?, 'ATTENDEE', 'ACTIVE', NOW())",
            name, email, phone
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private void insertCompletedBooking(Long userId, Double amount, String date) {
        jdbcTemplate.update("""
                INSERT INTO bookings (user_id, total_amount, status, booking_date, contact_email)
                VALUES (?, ?, 'COMPLETED', ?::timestamp, 'test@test.com')
                """, userId, amount, date + "T12:00:00");
    }
}
