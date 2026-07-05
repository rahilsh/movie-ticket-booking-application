package com.rsh.mtba.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.entity.User.Gender;
import com.rsh.mtba.repository.TheatreRepository;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TheatreControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TheatreRepository theatreRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtUtil jwtUtil;
  @Autowired private PasswordEncoder passwordEncoder;

  private String adminToken;
  private String userToken;

  @BeforeEach
  void setUp() {
    theatreRepository.deleteAll();
    userRepository.deleteAll();

    User admin =
        userRepository.save(
            User.builder()
                .name("Admin")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .gender(Gender.MALE)
                .role(User.Role.ROLE_ADMIN)
                .build());

    User regularUser =
        userRepository.save(
            User.builder()
                .name("User")
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("userpass"))
                .gender(Gender.FEMALE)
                .role(User.Role.ROLE_USER)
                .build());

    adminToken = jwtUtil.generateToken(admin.getEmail(), admin.getRole().name());
    userToken = jwtUtil.generateToken(regularUser.getEmail(), regularUser.getRole().name());
  }

  @Test
  @DisplayName("POST /api/theatres - admin can create theatre")
  void createTheatre_asAdmin_returns201() throws Exception {
    TheatreRequest request = new TheatreRequest();
    request.setName("PVR Cinemas");
    request.setAddress("MG Road");
    request.setCity("Bangalore");

    mockMvc
        .perform(
            post("/api/theatres")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("PVR Cinemas")))
        .andExpect(jsonPath("$.city", is("Bangalore")))
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @Test
  @DisplayName("POST /api/theatres - regular user gets 403")
  void createTheatre_asUser_returns403() throws Exception {
    TheatreRequest request = new TheatreRequest();
    request.setName("PVR Cinemas");
    request.setAddress("MG Road");
    request.setCity("Bangalore");

    mockMvc
        .perform(
            post("/api/theatres")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/theatres - unauthenticated request gets 403")
  void createTheatre_unauthenticated_returns403() throws Exception {
    TheatreRequest request = new TheatreRequest();
    request.setName("PVR Cinemas");
    request.setAddress("MG Road");
    request.setCity("Bangalore");

    mockMvc
        .perform(
            post("/api/theatres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/theatres - public endpoint returns all theatres")
  void getAllTheatres_public_returns200() throws Exception {
    // Create a theatre first
    TheatreRequest request = new TheatreRequest();
    request.setName("INOX");
    request.setAddress("Forum Mall");
    request.setCity("Bangalore");

    mockMvc
        .perform(
            post("/api/theatres")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Fetch without auth
    mockMvc
        .perform(get("/api/theatres"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("INOX")));
  }

  @Test
  @DisplayName("GET /api/theatres/{id} - returns 404 for non-existent theatre")
  void getTheatre_notFound_returns404() throws Exception {
    mockMvc
        .perform(get("/api/theatres/9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", is("Not Found")));
  }
}
