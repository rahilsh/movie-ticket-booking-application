package com.rsh.mtba.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.rsh.mtba.dto.request.RegisterRequest;
import com.rsh.mtba.dto.response.UserResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.entity.User.Gender;
import com.rsh.mtba.exception.DuplicateResourceException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  @Test
  @DisplayName("register() hashes password and saves user")
  void register_success() {
    RegisterRequest req = new RegisterRequest();
    req.setName("Alice");
    req.setEmail("alice@test.com");
    req.setPassword("plainpass");
    req.setGender(Gender.FEMALE);

    when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
    when(passwordEncoder.encode("plainpass")).thenReturn("hashed");
    User saved =
        User.builder()
            .id(1L)
            .name("Alice")
            .email("alice@test.com")
            .passwordHash("hashed")
            .gender(Gender.FEMALE)
            .role(User.Role.ROLE_USER)
            .build();
    when(userRepository.save(any())).thenReturn(saved);

    UserResponse resp = userService.register(req);

    assertThat(resp.getEmail()).isEqualTo("alice@test.com");
    assertThat(resp.getRole()).isEqualTo(User.Role.ROLE_USER);
    verify(passwordEncoder).encode("plainpass");
  }

  @Test
  @DisplayName("register() throws DuplicateResourceException on duplicate email")
  void register_duplicate_throws() {
    RegisterRequest req = new RegisterRequest();
    req.setName("Alice");
    req.setEmail("alice@test.com");
    req.setPassword("pass");
    req.setGender(Gender.FEMALE);

    when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

    assertThatThrownBy(() -> userService.register(req))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("alice@test.com");
  }

  @Test
  @DisplayName("getById() returns UserResponse for existing user")
  void getById_success() {
    User u =
        User.builder()
            .id(1L)
            .name("Bob")
            .email("bob@test.com")
            .passwordHash("h")
            .gender(Gender.MALE)
            .role(User.Role.ROLE_USER)
            .build();
    when(userRepository.findById(1L)).thenReturn(Optional.of(u));

    UserResponse resp = userService.getById(1L);
    assertThat(resp.getName()).isEqualTo("Bob");
  }

  @Test
  @DisplayName("getById() throws ResourceNotFoundException for unknown id")
  void getById_notFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> userService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("findByEmail() returns User for known email")
  void findByEmail_success() {
    User u =
        User.builder()
            .id(1L)
            .name("Carol")
            .email("carol@test.com")
            .passwordHash("h")
            .gender(Gender.FEMALE)
            .role(User.Role.ROLE_USER)
            .build();
    when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.of(u));

    User result = userService.findByEmail("carol@test.com");
    assertThat(result.getEmail()).isEqualTo("carol@test.com");
  }

  @Test
  @DisplayName("findByEmail() throws ResourceNotFoundException for unknown email")
  void findByEmail_notFound() {
    when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> userService.findByEmail("x@x.com"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
