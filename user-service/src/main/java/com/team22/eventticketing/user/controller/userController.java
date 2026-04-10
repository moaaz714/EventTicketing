package com.team22.eventticketing.user.controller;

import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
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

    public userController(UserService userService) {
        this.userService = userService;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(user));
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

   
    @GetMapping("/search")
    public ResponseEntity<List<User>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(userService.search(name, email, role));
    }
}
