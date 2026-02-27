package com.assignment.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "JWT token returned after successful login")
public class AuthResponse {

    @Schema(description = "JWT Bearer token — use as: Authorization: Bearer <token>",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqYW5lQGV4YW1wbGUuY29tIn0.xxx")
    private String token;
}
