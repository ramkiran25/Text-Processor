package com.textprocessor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.textprocessor.dto.LoginRequest;
import com.textprocessor.dto.LoginResponse;
import com.textprocessor.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class LoginController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
    LoginResponse response = authService.authenticate(loginRequest);

    if (response.success()) {
      log.info("Authentication granted for user: {}", loginRequest.username());
      return ResponseEntity.ok(response);
    } else {
      log.warn("Authentication rejected for user: {}", loginRequest.username());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
  }

}
