package com.team22.eventticketing.user.controller;

import com.team22.eventticketing.user.dto.UserProfileDTO;
import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.service.FavoriteVenueService;
import com.team22.eventticketing.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class userController {

    private final UserService userService;
    private final FavoriteVenueService favoriteVenueService;

    public userController(UserService userService, FavoriteVenueService favoriteVenueService) {
        this.userService = userService;
        this.favoriteVenueService = favoriteVenueService;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(user));
    }

    // ─── Read all ─────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<User>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

// ─── Read by ID ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

// ─── Update ───────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<User> update(
            @PathVariable Long id,
            @RequestBody User patch) {
        return ResponseEntity.ok(userService.update(id, patch));
    }

// ─── Delete ───────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── [S1-F2] Update User Preferences ─────────────────────────────────────────

    @PutMapping("/{id}/preferences")
    public ResponseEntity<User> updatePreferences(
            @PathVariable Long id,
            @RequestBody Map<String, Object> preferences) {
        return ResponseEntity.ok(userService.updatePreferences(id, preferences));
    }

    // ─── Health ───────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public String statusCheck() {
        return "OK";
    }

    // ─── [S1-F3] Search Users ─────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<List<User>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(userService.search(name, email, role));
    }

    // ─── [S1-F5] Filter Users by Preference (JSONB Query) ────────────────────────

    @GetMapping("/preferences/search")
    public ResponseEntity<List<User>> findByPreference(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(userService.findByPreference(key, value));
    }

    // ─── [S1-F4] Deactivate User Account ─────────────────────────────────────────

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<User> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    // ─── [S1-F7] Set Default Venue ───────────────────────────────────────────────

    @PutMapping("/{userId}/venues/{venueId}/default")
    public ResponseEntity<User> setDefaultVenue(
            @PathVariable Long userId,
            @PathVariable Long venueId) {
        return ResponseEntity.ok(favoriteVenueService.setDefault(userId, venueId));
    }

    // ─── [S1-F8] Get User Profile ────────────────────────────────────────────────

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDTO> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }

    // ─── [S1-F9] Filter by Favorite Category ─────────────────────────────────────

    @GetMapping("/preferences/category")
    public ResponseEntity<List<User>> findByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int minBookings) {
        return ResponseEntity.ok(
                userService.findByFavoriteCategoryAndMinBookings(category, minBookings));
    }
}
