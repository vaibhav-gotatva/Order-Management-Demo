package com.assignment.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Credentials for authentication")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
    @Schema(description = "Registered email address", example = "jane@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "secret123")
    private String password;
}
