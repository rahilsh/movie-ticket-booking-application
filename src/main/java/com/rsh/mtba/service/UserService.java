package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.RegisterRequest;
import com.rsh.mtba.dto.response.UserResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.DuplicateResourceException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new DuplicateResourceException(
          "User with email " + request.getEmail() + " already exists");
    }
    User user = User.builder()
        .name(request.getName())
        .email(request.getEmail())
        .passwordHash(passwordEncoder.encode(request.getPassword()))
        .phone(request.getPhone())
        .gender(request.getGender())
        .role(User.Role.ROLE_USER)
        .build();
    User saved = userRepository.save(user);
    log.info("Registered new user with id={} email={}", saved.getId(), saved.getEmail());
    return UserResponse.from(saved);
  }

  public UserResponse getById(Long id) {
    return UserResponse.from(findById(id));
  }

  public User findById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  public User findByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }
}
