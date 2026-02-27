package com.assignment.demo.controller;

import com.assignment.demo.dto.AuthResponse;
import com.assignment.demo.dto.LoginRequest;
import com.assignment.demo.dto.RegisterRequest;
import com.assignment.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Authentication", description = "Register a new user and obtain a JWT token")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Role must be one of: ADMIN, USER.",
        security = {}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"message\": \"User registered successfully\"}"))),
        @ApiResponse(responseCode = "400", description = "Validation error — missing fields or invalid role",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Invalid role\"}"))),
        @ApiResponse(responseCode = "400", description = "Email already registered",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Email already in use\"}")))
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", message));
    }

    @Operation(
        summary = "Login and obtain JWT",
        description = "Authenticates with email and password. Returns a JWT valid for 1 hour.",
        security = {}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — JWT returned",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Bad credentials\"}")))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
