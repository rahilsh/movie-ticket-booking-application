package com.rsh.mtba.controller;

import com.rsh.mtba.dto.request.LoginRequest;
import com.rsh.mtba.dto.request.RegisterRequest;
import com.rsh.mtba.dto.response.AuthResponse;
import com.rsh.mtba.dto.response.UserResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.security.JwtUtil;
import com.rsh.mtba.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  @PostMapping("/register")
  @Operation(summary = "Register a new user")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    UserResponse user = userService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  @PostMapping("/login")
  @Operation(summary = "Login and receive a JWT token")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    Authentication auth =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    String email = auth.getName();
    User user = userRepository.findByEmail(email).orElseThrow();
    String token = jwtUtil.generateToken(email);

    return ResponseEntity.ok(
        AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .user(UserResponse.from(user))
            .build());
  }
}
