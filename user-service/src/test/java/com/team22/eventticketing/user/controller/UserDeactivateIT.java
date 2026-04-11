package com.team22.eventticketing.user.controller;

import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.entity.UserStatus;
import com.team22.eventticketing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserDeactivateIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        jdbcTemplate.execute("DELETE FROM bookings WHERE user_id IN (SELECT id FROM users WHERE email = 'deactivate-test@test.com')");
        jdbcTemplate.execute("DELETE FROM users WHERE email = 'deactivate-test@test.com'");

        User user = new User("Deactivate Test", "deactivate-test@test.com", "pass", "0000000000", Role.ATTENDEE);
        userId = userRepository.save(user).getId();

        // Scenario a: create a PENDING booking for this user
        jdbcTemplate.update("""
                INSERT INTO bookings (user_id, total_amount, status, booking_date, contact_email)
                VALUES (?, 100.0, 'PENDING', NOW(), 'test@test.com')
                """, userId);
    }

    // Scenario b: deactivate with active booking → 400
    @Test
    void deactivate_returns400WhenActiveBookingExists() throws Exception {
        mockMvc.perform(put("/api/users/" + userId + "/deactivate"))
                .andExpect(status().isBadRequest());
    }

    // Scenario d: deactivate after booking completed → success
    @Test
    void deactivate_succeedsWhenNoActiveBookings() throws Exception {
        // Scenario c: update booking to COMPLETED
        jdbcTemplate.update("UPDATE bookings SET status = 'COMPLETED' WHERE user_id = ?", userId);

        mockMvc.perform(put("/api/users/" + userId + "/deactivate"))
                .andExpect(status().isOk());

        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
    }

    // 404 when user not found
    @Test
    void deactivate_returns404WhenUserNotFound() throws Exception {
        mockMvc.perform(put("/api/users/999999/deactivate"))
                .andExpect(status().isNotFound());
    }
}
