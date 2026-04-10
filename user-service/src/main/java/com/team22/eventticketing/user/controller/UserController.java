package com.team22.eventticketing.user.controller;

import com.team22.eventticketing.user.dto.UserBookingSummaryDTO;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return new ResponseEntity<>(userService.create(user), HttpStatus.CREATED);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/preferences/search")
    public ResponseEntity<List<User>> findByPreference(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(userService.findByPreference(key, value));
    }

    // ─── [S1-F3] Get User Booking Summary ────────────────────────────────────────

    @GetMapping("/{id}/booking-summary")
    public ResponseEntity<UserBookingSummaryDTO> getBookingSummary(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getBookingSummary(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.update(id, user));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<User> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
