package com.team22.eventticketing.user.service;

import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
