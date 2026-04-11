package com.team22.eventticketing.user;

import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.entity.UserStatus;
import com.team22.eventticketing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM bookings WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test-s1f6-%')");
        jdbcTemplate.execute("DELETE FROM users WHERE email LIKE 'test-s1f6-%'");

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

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private Long createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("password");
        user.setPhone("0000000000");
        user.setRole(Role.ATTENDEE);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user).getId();
    }

    private void insertCompletedBooking(Long userId, Double amount, String date) {
        jdbcTemplate.update("""
                INSERT INTO bookings (user_id, total_amount, status, booking_date)
                VALUES (?, ?, 'COMPLETED', ?::timestamp)
                """, userId, amount, date + "T12:00:00");
    }
}
