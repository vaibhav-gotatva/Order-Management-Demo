package com.assignment.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Credentials for authentication")
public class LoginRequest {

    @Schema(description = "Registered email address", example = "jane@example.com")
    private String email;

    @Schema(description = "Account password", example = "secret123")
    private String password;
}
