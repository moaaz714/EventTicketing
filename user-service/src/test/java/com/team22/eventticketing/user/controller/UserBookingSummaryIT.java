package com.team22.eventticketing.user.controller;

import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// [S1-F3] Get User Booking Summary (DTO)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserBookingSummaryIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long emptyUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        User user = new User("Summary User", "s1f3-summary@test.com", "pass", "0300000001", Role.ATTENDEE);
        userId = userRepository.save(user).getId();

        User emptyUser = new User("Empty User", "s1f3-empty@test.com", "pass", "0300000002", Role.ATTENDEE);
        emptyUserId = userRepository.save(emptyUser).getId();

        // 3 COMPLETED: 300, 500, 700
        insertBooking(userId, 300.0, "COMPLETED");
        insertBooking(userId, 500.0, "COMPLETED");
        insertBooking(userId, 700.0, "COMPLETED");
        // 1 CANCELLED
        insertBooking(userId, 100.0, "CANCELLED");
        // 1 PENDING
        insertBooking(userId, 200.0, "PENDING");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM bookings WHERE user_id IN (?, ?)", userId, emptyUserId);
        userRepository.deleteById(userId);
        userRepository.deleteById(emptyUserId);
    }

    // (b) full summary
    @Test
    void bookingSummary_returnsCorrectAggregates() throws Exception {
        mockMvc.perform(get("/api/users/" + userId + "/booking-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(5))
                .andExpect(jsonPath("$.completedBookings").value(3))
                .andExpect(jsonPath("$.cancelledBookings").value(1))
                .andExpect(jsonPath("$.totalSpent").value(1500.0))
                .andExpect(jsonPath("$.averageBookingAmount").value(500.0));
    }

    // (c) user with no bookings → all zeros
    @Test
    void bookingSummary_noBookings_returnsAllZeros() throws Exception {
        mockMvc.perform(get("/api/users/" + emptyUserId + "/booking-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(0))
                .andExpect(jsonPath("$.completedBookings").value(0))
                .andExpect(jsonPath("$.cancelledBookings").value(0))
                .andExpect(jsonPath("$.totalSpent").value(0.0))
                .andExpect(jsonPath("$.averageBookingAmount").value(0.0));
    }

    // (d) non-existent user → 404
    @Test
    void bookingSummary_nonExistentUser_returns404() throws Exception {
        mockMvc.perform(get("/api/users/999999/booking-summary"))
                .andExpect(status().isNotFound());
    }

    private void insertBooking(Long userId, Double amount, String status) {
        jdbcTemplate.update("""
                INSERT INTO bookings (user_id, total_amount, status, booking_date, contact_email)
                VALUES (?, ?, ?, NOW(), 'test@test.com')
                """, userId, amount, status);
    }
}
