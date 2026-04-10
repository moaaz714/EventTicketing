package com.team22.eventticketing.user.service;

import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.dto.UserBookingSummaryDTO;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public User create(User user) {
        return userRepository.save(user);
    }

    // ─── Read by ID ───────────────────────────────────────────────────────────────

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    // ─── Read all ─────────────────────────────────────────────────────────────────

    public List<User> findAll() {
        return userRepository.findAll();
    }

    // ─── Update ───────────────────────────────────────────────────────────────────

    public User update(Long id, User patch) {
        User existing = findById(id);

        if (patch.getName() != null)        existing.setName(patch.getName());
        if (patch.getEmail() != null)       existing.setEmail(patch.getEmail());
        if (patch.getPassword() != null)    existing.setPassword(patch.getPassword());
        if (patch.getPhone() != null)       existing.setPhone(patch.getPhone());
        if (patch.getRole() != null)        existing.setRole(patch.getRole());
        if (patch.getStatus() != null)      existing.setStatus(patch.getStatus());
        if (patch.getPreferences() != null) existing.setPreferences(patch.getPreferences());

        return userRepository.save(existing);
    }

    // ─── Update Preferences ───────────────────────────────────────────────────────

    public User updatePreferences(Long id, Map<String, Object> incoming) {
        User user = findById(id);
        user.getPreferences().putAll(incoming);
        return userRepository.save(user);
    }

    // ─── Search ───────────────────────────────────────────────────────────────────

    public List<User> search(String name, String email, Role role) {
        return userRepository.searchUsers(name, email, role == null ? null : role.name());
    // ─── Filter by Preference (JSONB) ────────────────────────────────────────────

    /**
     * Returns all users whose {@code preferences} JSONB column contains an entry
     * where the given {@code key} maps to the given {@code value}.
     *
     * Throws 400 Bad Request if either {@code key} or {@code value} is blank.
     */
    public List<User> findByPreference(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Query parameter 'key' must not be blank");
        }
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Query parameter 'value' must not be blank");
        }
        return userRepository.findByPreference(key, value);
    }

    // ─── [S1-F3] Get User Booking Summary (DTO) ──────────────────────────────────

    public UserBookingSummaryDTO getBookingSummary(Long id) {
        User user = findById(id); // throws 404 if not found

        Object[] row = userRepository.getBookingSummaryByUserId(id).get(0);

        long totalBookings        = ((Number) row[0]).longValue();
        long completedBookings    = ((Number) row[1]).longValue();
        long cancelledBookings    = ((Number) row[2]).longValue();
        double totalSpent         = ((Number) row[3]).doubleValue();
        double averageBookingAmount = ((Number) row[4]).doubleValue();

        return new UserBookingSummaryDTO(
                user.getId(),
                user.getName(),
                totalBookings,
                completedBookings,
                cancelledBookings,
                totalSpent,
                averageBookingAmount
        );
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // ─── Deactivate ─────────────────────────────────────────────────────────────

    @org.springframework.transaction.annotation.Transactional
    public User deactivateUser(Long id) {
        User user = findById(id);

        long activeBookings = userRepository.countActiveBookingsByUserId(id);
        if (activeBookings > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has active bookings");
        }

        user.setStatus(com.team22.eventticketing.user.entity.UserStatus.DEACTIVATED);
        return userRepository.save(user);
    }
}
