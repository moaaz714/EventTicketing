package com.team22.eventticketing.user.service;

import com.team22.eventticketing.user.dto.AuthResponse;
import com.team22.eventticketing.user.dto.LoginRequest;
import com.team22.eventticketing.user.dto.RegisterRequest;
import com.team22.eventticketing.user.entity.Role;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.entity.UserStatus;
import com.team22.eventticketing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ─── [S1-F10] Register ────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {

        // a) Validate fields are not blank
        if (request.name() == null || request.name().isBlank() ||
                request.email() == null || request.email().isBlank() ||
                request.password() == null || request.password().isBlank() ||
                request.phone() == null || request.phone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Name, email, password, and phone must not be blank");
        }

        // b) Check email and phone uniqueness
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phone already registered");
        }

        // c) Hash password
        String hashedPassword = passwordEncoder.encode(request.password());

        // d) Create user with ATTENDEE role and ACTIVE status
        User user = new User(
                request.name(),
                request.email(),
                hashedPassword,
                request.phone(),
                Role.ATTENDEE
        );
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // e) MongoDB logging skipped for now

        // f) Generate token
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // g) Return 201
        return new AuthResponse(token, jwtService.getExpirationMs());
    }

    // ─── [S1-F11] Login ───────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {

        // a) Find user by email — 401 if not found (do not reveal whether email exists)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // b) Verify password — 401 if wrong (same message to prevent enumeration)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // c) MongoDB logging skipped for now

        // d) Generate token
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // e) Return 200
        return new AuthResponse(token, jwtService.getExpirationMs());
    }
}