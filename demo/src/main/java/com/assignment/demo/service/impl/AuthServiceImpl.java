package com.assignment.demo.service.impl;

import com.assignment.demo.dto.AuthResponse;
import com.assignment.demo.dto.LoginRequest;
import com.assignment.demo.dto.RegisterRequest;
import com.assignment.demo.entity.User;
import com.assignment.demo.entity.UserRole;
import com.assignment.demo.enums.Role;
import com.assignment.demo.repository.UserRepository;
import com.assignment.demo.repository.UserRoleRepository;
import com.assignment.demo.security.JwtService;
import com.assignment.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public String register(RegisterRequest req) {
        // ── fullName ──────────────────────────────────────────────────────────
        String fullName = req.getFullName() == null ? null : req.getFullName().trim();
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Full name is required");
        if (fullName.length() > 100)
            throw new IllegalArgumentException("Full name must not exceed 100 characters");

        // ── email ─────────────────────────────────────────────────────────────
        String email = req.getEmail() == null ? null : req.getEmail().trim();
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required");
        if (!email.matches(EMAIL_REGEX))
            throw new IllegalArgumentException("Invalid email format");
        email = email.toLowerCase();

        // ── password ──────────────────────────────────────────────────────────
        String password = req.getPassword() == null ? null : req.getPassword().trim();
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password is required");
        if (password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters");
        if (password.length() > 72)
            throw new IllegalArgumentException("Password must not exceed 72 characters");

        // ── role ──────────────────────────────────────────────────────────────
        Role roleEnum;
        String rawRole = req.getRole() == null ? null : req.getRole().trim();
        if (rawRole == null || rawRole.isBlank()) {
            roleEnum = Role.USER;
        } else {
            try {
                roleEnum = Role.valueOf(rawRole.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role. Accepted values: ADMIN, USER, VIEWER");
            }
        }

        // ── duplicate email ───────────────────────────────────────────────────
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email is already registered");

        // ── resolve UserRole (create if not yet seeded) ───────────────────────
        UserRole userRole = userRoleRepository.findByName(roleEnum)
                .orElseGet(() -> userRoleRepository.save(
                        UserRole.builder().name(roleEnum).build()
                ));

        // ── persist ───────────────────────────────────────────────────────────
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        // ── email ─────────────────────────────────────────────────────────────
        String email = req.getEmail() == null ? null : req.getEmail().trim();
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required");
        if (!email.matches(EMAIL_REGEX))
            throw new IllegalArgumentException("Invalid email format");
        email = email.toLowerCase();

        // ── password ──────────────────────────────────────────────────────────
        String password = req.getPassword() == null ? null : req.getPassword().trim();
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password is required");

        // ── authenticate ──────────────────────────────────────────────────────
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }
}
