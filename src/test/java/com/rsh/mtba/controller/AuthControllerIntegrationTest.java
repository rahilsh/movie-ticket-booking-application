package com.rsh.mtba.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.rsh.mtba.dto.request.LoginRequest;
import com.rsh.mtba.dto.request.RegisterRequest;
import com.rsh.mtba.entity.User.Gender;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.util.TestDataCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private TestDataCleaner cleaner;

  @BeforeEach
  void cleanDb() {
    cleaner.clean();
  }

  @AfterEach
  void tearDown() {
    cleaner.clean();
  }

  @Test
  @DisplayName("POST /api/auth/register - creates user and returns 201")
  void register_success() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setName("Alice");
    request.setEmail("alice@example.com");
    request.setPassword("securepassword123");
    request.setGender(Gender.FEMALE);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email", is("alice@example.com")))
        .andExpect(jsonPath("$.name", is("Alice")))
        .andExpect(jsonPath("$.role", is("ROLE_USER")));
  }

  @Test
  @DisplayName("POST /api/auth/register - returns 409 on duplicate email")
  void register_duplicateEmail_returns409() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setName("Alice");
    request.setEmail("alice@example.com");
    request.setPassword("securepassword123");
    request.setGender(Gender.FEMALE);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Second registration with same email
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error", is("Conflict")));
  }

  @Test
  @DisplayName("POST /api/auth/register - returns 400 for invalid payload")
  void register_invalidPayload_returns400() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setName(""); // blank name
    request.setEmail("not-an-email");
    request.setPassword("short"); // too short

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors", notNullValue()));
  }

  @Test
  @DisplayName("POST /api/auth/login - returns JWT token on valid credentials")
  void login_success() throws Exception {
    // Register first
    RegisterRequest reg = new RegisterRequest();
    reg.setName("Bob");
    reg.setEmail("bob@example.com");
    reg.setPassword("securepassword123");
    reg.setGender(Gender.MALE);
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
        .andExpect(status().isCreated());

    // Login
    LoginRequest login = new LoginRequest();
    login.setEmail("bob@example.com");
    login.setPassword("securepassword123");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", notNullValue()))
        .andExpect(jsonPath("$.tokenType", is("Bearer")))
        .andExpect(jsonPath("$.user.email", is("bob@example.com")));
  }

  @Test
  @DisplayName("POST /api/auth/login - returns 401 on wrong password")
  void login_wrongPassword_returns401() throws Exception {
    RegisterRequest reg = new RegisterRequest();
    reg.setName("Carol");
    reg.setEmail("carol@example.com");
    reg.setPassword("correctpassword");
    reg.setGender(Gender.FEMALE);
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
        .andExpect(status().isCreated());

    LoginRequest login = new LoginRequest();
    login.setEmail("carol@example.com");
    login.setPassword("wrongpassword");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
        .andExpect(status().isUnauthorized());
  }
}
