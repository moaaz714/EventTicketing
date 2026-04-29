package com.team22.eventticketing.user.service;

import com.team22.eventticketing.user.dto.UserBookingSummaryDTO;
import com.team22.eventticketing.user.dto.TopAttendeeDTO;
import com.team22.eventticketing.user.dto.UserProfileDTO;
import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CacheEvictionService cacheEviction;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserService(UserRepository userRepository, CacheEvictionService cacheEviction) {
        this.userRepository = userRepository;
        this.cacheEviction = cacheEviction;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public User create(User user) {
        return userRepository.save(user);
    }

    // ─── [CRUD] Read by ID ────────────────────────────────────────────────────────

    @Cacheable(cacheNames = "user-service::user", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    // ─── Read all (NOT cached — list endpoints hit PG directly) ───────────────────

    public List<User> findAll() {
        return userRepository.findAll();
    }

    // ─── [CRUD] Update ────────────────────────────────────────────────────────────

    public User update(Long id, User patch) {
        User existing = findById(id);

        if (patch.getName() != null)        existing.setName(patch.getName());
        if (patch.getEmail() != null)       existing.setEmail(patch.getEmail());
        if (patch.getPassword() != null)    existing.setPassword(patch.getPassword());
        if (patch.getPhone() != null)       existing.setPhone(patch.getPhone());
        if (patch.getRole() != null)        existing.setRole(patch.getRole());
        if (patch.getStatus() != null)      existing.setStatus(patch.getStatus());
        if (patch.getPreferences() != null) existing.setPreferences(patch.getPreferences());

        User saved = userRepository.save(existing);
        cacheEviction.evictEntityAndFeatures("user", id, "S1-F1", "S1-F3", "S1-F5", "S1-F8", "S1-F9");
        return saved;
    }

    // ─── [S1-F2] Update Preferences ───────────────────────────────────────────────

    public User updatePreferences(Long id, Map<String, Object> incoming) {
        User user = findById(id);
        user.getPreferences().putAll(incoming);
        User saved = userRepository.save(user);
        cacheEviction.evictEntityAndFeatures("user", id, "S1-F1", "S1-F3", "S1-F5", "S1-F8", "S1-F9");
        return saved;
    }

    // ─── [S1-F1] Search ───────────────────────────────────────────────────────────

    @Cacheable(cacheNames = "user-service::S1-F1",
            key = "T(String).valueOf(#name) + ':' + T(String).valueOf(#email) + ':' + (#role == null ? 'null' : #role.name())")
    public List<User> search(String name, String email, Role role) {
        return userRepository.searchUsers(name, email, role == null ? null : role.name());
    }

    // ─── [S1-F5] Filter by Preference (JSONB) ────────────────────────────────────

    @Cacheable(cacheNames = "user-service::S1-F5", key = "#key + ':' + #value")
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

    @Cacheable(cacheNames = "user-service::S1-F3", key = "#id")
    public UserBookingSummaryDTO getBookingSummary(Long id) {
        User user = findById(id);

        Object[] row = userRepository.getBookingSummaryByUserId(id).get(0);

        long totalBookings          = ((Number) row[0]).longValue();
        long completedBookings      = ((Number) row[1]).longValue();
        long cancelledBookings      = ((Number) row[2]).longValue();
        double totalSpent           = ((Number) row[3]).doubleValue();
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

    // ─── [S1-F6] Top attendees by spending ────────────────────────────────────────

    @Cacheable(cacheNames = "user-service::S1-F6",
            key = "#startDate + ':' + #endDate + ':' + #limit")
    public List<TopAttendeeDTO> getTopAttendees(String startDate, String endDate, int limit) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end   = LocalDate.parse(endDate);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        return userRepository.findTopAttendees(start.atStartOfDay(), end.atTime(23, 59, 59), limit)
                .stream()
                .map(row -> new TopAttendeeDTO(
                        ((Number) row[0]).longValue(),
                        (String)  row[1],
                        ((Number) row[2]).doubleValue(),
                        ((Number) row[3]).longValue()
                ))
                .collect(Collectors.toList());
    }

    // ─── [CRUD] Delete ────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        userRepository.deleteById(id);
        cacheEviction.evictEntityAndFeatures("user", id, "S1-F1", "S1-F3", "S1-F5", "S1-F6", "S1-F8", "S1-F9");
    }

    // ─── [S1-F4] Deactivate ──────────────────────────────────────────────────────

    @org.springframework.transaction.annotation.Transactional
    public User deactivateUser(Long id) {
        User user = findById(id);

        long activeBookings = userRepository.countActiveBookingsByUserId(id);
        if (activeBookings > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has active bookings");
        }

        user.setStatus(com.team22.eventticketing.user.entity.UserStatus.DEACTIVATED);
        User saved = userRepository.save(user);
        cacheEviction.evictEntityAndFeatures("user", id, "S1-F1", "S1-F3", "S1-F5", "S1-F6", "S1-F8", "S1-F9");
        return saved;
    }

    // ─── [S1-F8] Get User Profile ────────────────────────────────────────────────

    @Cacheable(cacheNames = "user-service::S1-F8", key = "#id")
    public UserProfileDTO getProfile(Long id) {
        User user = userRepository.findByIdWithVenues(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));
        return UserProfileDTO.from(user);
    }

    // ─── [S1-F9] Filter by Favorite Category ─────────────────────────────────────

    @Cacheable(cacheNames = "user-service::S1-F9",
            key = "#category + ':' + #minBookings")
    public List<User> findByFavoriteCategoryAndMinBookings(String category, int minBookings) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category must not be blank");
        }

        String categoryJson;
        try {
            categoryJson = objectMapper.writeValueAsString(
                    Map.of("favoriteCategory", category));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to build category filter");
        }

        return userRepository.findByFavoriteCategoryAndMinBookings(categoryJson, minBookings);
    }
}
