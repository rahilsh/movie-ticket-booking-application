package com.rsh.mtba.controller;

import com.rsh.mtba.dto.response.UserResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
public class UserController {

  private final UserService userService;
  private final UserRepository userRepository;

  @GetMapping("/me")
  @Operation(summary = "Get the authenticated user's profile")
  public ResponseEntity<UserResponse> getMe(Authentication authentication) {
    User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
    return ResponseEntity.ok(UserResponse.from(user));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get user by ID (Admin only)")
  public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getById(id));
  }
}
