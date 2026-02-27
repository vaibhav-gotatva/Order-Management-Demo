package com.assignment.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Payload to register a new user account")
public class RegisterRequest {

    @Schema(description = "Full name of the user", example = "Jane Doe")
    private String fullName;

    @Schema(description = "Email address (used as login username)", example = "jane@example.com")
    private String email;

    @Schema(description = "Account password", example = "secret123")
    private String password;

    @Schema(description = "Account role", example = "USER", allowableValues = {"ADMIN", "USER"})
    private String role;
}
