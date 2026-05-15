package com.team22.eventticketing.user.service;

import com.team22.eventticketing.user.dto.ActivityFeedResponse;
import com.team22.eventticketing.user.dto.AuthEventDTO;
import com.team22.eventticketing.user.dto.UserBookingSummaryDTO;
import com.team22.eventticketing.user.dto.TopAttendeeDTO;
import com.team22.eventticketing.user.dto.UserProfileDTO;
import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.messaging.UserEventPublisher;
import com.team22.eventticketing.user.mongo.AuthEvent;
import com.team22.eventticketing.user.observer.EntityObserver;
import com.team22.eventticketing.user.repository.AuthEventRepository;
import com.team22.eventticketing.user.repository.UserRepository;
import feign.BookingServiceClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthEventRepository authEventRepository;
    private final CacheEvictionService cacheEviction;
    private final BookingServiceClient bookingServiceClient;
    private final UserEventPublisher userEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<EntityObserver> observers = new ArrayList<>();

    public UserService(UserRepository userRepository,
                       AuthEventRepository authEventRepository,
                       CacheEvictionService cacheEviction,
                       BookingServiceClient bookingServiceClient,
                       UserEventPublisher userEventPublisher) {
        this.userRepository = userRepository;
        this.authEventRepository = authEventRepository;
        this.cacheEviction = cacheEviction;
        this.bookingServiceClient = bookingServiceClient;
        this.userEventPublisher = userEventPublisher;
    }

    public void register(EntityObserver observer) { observers.add(observer); }
    public void unregister(EntityObserver observer) { observers.remove(observer); }

    private void notifyObservers(String action, Object payload) {
        for (EntityObserver o : observers) o.onEvent(action, payload);
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public User create(User user) {
        User saved = userRepository.save(user);
        notifyObservers("REGISTERED", Map.of("userId", saved.getId(), "email", saved.getEmail() != null ? saved.getEmail() : ""));
        return saved;
    }

    public Long getIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Unknown caller"))
                .getId();
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
        notifyObservers("USER_UPDATED", Map.of("userId", saved.getId()));
        return saved;
    }

    // ─── [S1-F2] Update Preferences ───────────────────────────────────────────────

    public User updatePreferences(Long id, Map<String, Object> incoming) {
        User user = findById(id);
        user.getPreferences().putAll(incoming);
        User saved = userRepository.save(user);
        cacheEviction.evictEntityAndFeatures("user", id, "S1-F1", "S1-F3", "S1-F5", "S1-F8", "S1-F9");
        notifyObservers("PREFERENCES_UPDATED", Map.of("userId", saved.getId()));
        return saved;
    }

    // ─── [CC-2] Update Role ───────────────────────────────────────────────────────

    public User updateRole(Long id, String roleStr) {
        Role newRole;
        try {
            newRole = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role value: " + roleStr);
        }

        User user = findById(id);
        String oldRole = user.getRole().name();
        user.setRole(newRole);
        User saved = userRepository.save(user);
        cacheEviction.evictDetail("user", id);
        cacheEviction.evictPattern("S1-F12::" + id);
        notifyObservers("ROLE_CHANGED", Map.of(
                "userId", saved.getId(),
                "oldRole", oldRole,
                "newRole", newRole.name()
        ));
        return saved;
    }

    // ─── [S1-F12] Get User Activity Feed ─────────────────────────────────────────

    @Cacheable(cacheNames = "user-service::S1-F12", key = "#userId + '::' + #page + '::' + #size")
    public ActivityFeedResponse getActivityFeed(Long userId, int page, int size) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        if (size <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be > 0");
        if (size > 100) size = 100;

        Pageable pageable = PageRequest.of(page, size);
        Page<AuthEvent> events = authEventRepository.findByUserIdOrderByTimestampDesc(userId, pageable);

        List<AuthEventDTO> content = events.getContent().stream()
                .map(e -> new AuthEventDTO(
                        e.getId(),
                        e.getTimestamp(),
                        e.getAction(),
                        e.getDetails(),
                        e.getUserId(),
                        e.getEmail()))
                .collect(Collectors.toList());

        return new ActivityFeedResponse(content, page, size, events.getTotalElements());
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
        return userRepository.findAllUsers().stream()
                .filter(u -> {
                    Object v = u.getPreferences().get(key);
                    return v != null && v.toString().equals(value);
                })
                .collect(Collectors.toList());
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

        return UserBookingSummaryDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .totalSpent(totalSpent)
                .averageBookingAmount(averageBookingAmount)
                .build();
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
                .map(row -> TopAttendeeDTO.builder()
                        .userId(((Number) row[0]).longValue())
                        .name((String)  row[1])
                        .totalSpent(((Number) row[2]).doubleValue())
                        .bookingCount(((Number) row[3]).longValue())
                        .build()).collect(Collectors.toList());
    }

    // ─── [CRUD] Delete ────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        userRepository.deleteById(id);
        cacheEviction.evictEntityAndFeatures("user", id, "S1-F1", "S1-F3", "S1-F5", "S1-F6", "S1-F8", "S1-F9");
        notifyObservers("USER_DELETED", Map.of("userId", id));
    }

    // ─── [S1-F4] Deactivate ──────────────────────────────────────────────────────

    @org.springframework.transaction.annotation.Transactional
    public User deactivateUser(Long id) {
        User user = findById(id);

        int activeBookings = bookingServiceClient.getActiveBookingCount(id);
        if (activeBookings > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has active bookings");
        }

        user.setStatus(com.team22.eventticketing.user.entity.UserStatus.DEACTIVATED);
        User saved = userRepository.save(user);
        cacheEviction.evictEntityAndFeatures("user", id, "S1-F1", "S1-F3", "S1-F5", "S1-F6", "S1-F8", "S1-F9");
        notifyObservers("USER_DEACTIVATED", Map.of("userId", saved.getId()));
        userEventPublisher.publishUserDeactivated(saved.getId());
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

        Map<Long, Long> completedCounts = userRepository.countCompletedBookingsPerUser()
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        return userRepository.findAllUsers().stream()
                .filter(u -> {
                    Object v = u.getPreferences().get("favoriteCategory");
                    return v != null && v.toString().equals(category);
                })
                .filter(u -> completedCounts.getOrDefault(u.getId(), 0L) >= minBookings)
                .collect(Collectors.toList());
    }
}
