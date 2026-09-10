package com.textprocessor.service;

import org.springframework.stereotype.Service;
import com.textprocessor.dto.LoginRequest;
import com.textprocessor.dto.LoginResponse;


@Service
public class AuthService {
  public LoginResponse authenticate(LoginRequest loginRequest) {
    String username = loginRequest.username();
    String password = loginRequest.password();
    if (username == null || username.trim().isEmpty() || password == null
        || password.trim().isEmpty()) {
      return new LoginResponse(false, "username and password invalid");
    }

    if ("admin".equalsIgnoreCase(username.trim()) && "admin".equalsIgnoreCase(password)) {
      String mockToken = "mock-jwt-token-xyz-789";
      return new LoginResponse(true, "Authentication successful!");
    }
    return new LoginResponse(false, "username and password invalid");

  }

}
