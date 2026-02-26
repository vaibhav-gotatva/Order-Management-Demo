package com.assignment.demo.service;

import com.assignment.demo.dto.AuthResponse;
import com.assignment.demo.dto.LoginRequest;
import com.assignment.demo.dto.RegisterRequest;

public interface AuthService {
    String register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
}
