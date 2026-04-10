package com.team22.eventticketing.user.repository;

import com.team22.eventticketing.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users WHERE " +
           "(:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:role IS NULL OR role = :role)",
           nativeQuery = true)
    List<User> searchUsers(@Param("name") String name,
                           @Param("email") String email,
                           @Param("role") String role);
    // S1-F3: aggregate booking stats for a user from the shared bookings table
    @Query(value = """
        SELECT
            COUNT(b.id)                                                          AS totalBookings,
            COALESCE(SUM(CASE WHEN b.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedBookings,
            COALESCE(SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelledBookings,
            COALESCE(SUM(CASE WHEN b.status = 'COMPLETED' THEN b.total_amount ELSE 0 END), 0) AS totalSpent,
            COALESCE(AVG(CASE WHEN b.status = 'COMPLETED' THEN b.total_amount END), 0)        AS averageBookingAmount
        FROM bookings b
        WHERE b.user_id = :userId
        """, nativeQuery = true)
    List<Object[]> getBookingSummaryByUserId(@Param("userId") Long userId);

    // S1-F4: count active (non-terminal) bookings for a user in the shared bookings table
    @Query(value = """
        SELECT COUNT(*) FROM bookings
        WHERE user_id = :userId
          AND status NOT IN ('COMPLETED', 'CANCELLED')
        """, nativeQuery = true)
    long countActiveBookingsByUserId(@Param("userId") Long userId);

    // S1-F5: filter users by a JSONB preferences key-value pair
    @Query(value = "SELECT * FROM users WHERE preferences ->> :key = :value",
           nativeQuery = true)
    List<User> findByPreference(@Param("key") String key,
                                @Param("value") String value);
}
