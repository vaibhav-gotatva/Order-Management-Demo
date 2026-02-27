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

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public String register(RegisterRequest req) {
        // Null/blank/size/format checks are handled by Bean Validation (@NotBlank, @Size, @Pattern)
        // Trim and normalize values for persistence
        String fullName = req.getFullName().trim();
        String email = req.getEmail().trim().toLowerCase();
        String password = req.getPassword().trim();

        // ── role ──────────────────────────────────────────────────────────────
        Role roleEnum;
        String rawRole = req.getRole() == null ? null : req.getRole().trim();
        if (rawRole == null || rawRole.isBlank()) {
            roleEnum = Role.USER;
        } else {
            try {
                roleEnum = Role.valueOf(rawRole.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role. Accepted values: ADMIN, USER");
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
        // Null/blank/format checks are handled by Bean Validation (@NotBlank, @Pattern)
        String email = req.getEmail().trim().toLowerCase();
        String password = req.getPassword().trim();

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
