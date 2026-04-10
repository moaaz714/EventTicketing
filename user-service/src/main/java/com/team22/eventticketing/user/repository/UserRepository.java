package com.team22.eventticketing.user.repository;

import com.team22.eventticketing.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // S1-F4: count active (non-terminal) bookings for a user in the shared bookings table
    @Query(value = """
        SELECT COUNT(*) FROM bookings
        WHERE user_id = :userId
          AND status NOT IN ('COMPLETED', 'CANCELLED')
        """, nativeQuery = true)
    long countActiveBookingsByUserId(@Param("userId") Long userId);
}
