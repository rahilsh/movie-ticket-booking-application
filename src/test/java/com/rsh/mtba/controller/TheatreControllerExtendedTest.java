package com.rsh.mtba.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.entity.User.Gender;
import com.rsh.mtba.repository.TheatreRepository;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.security.JwtUtil;
import com.rsh.mtba.util.TestDataCleaner;
import org.junit.jupiter.api.AfterEach;
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
class TheatreControllerExtendedTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TheatreRepository theatreRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtUtil jwtUtil;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataCleaner cleaner;

  private String adminToken;

  @BeforeEach
  void setUp() {
    cleaner.clean();

    User admin =
        userRepository.save(
            User.builder()
                .name("Admin")
                .email("admin2@example.com")
                .passwordHash(passwordEncoder.encode("pass"))
                .gender(Gender.MALE)
                .role(User.Role.ROLE_ADMIN)
                .build());

    adminToken = jwtUtil.generateToken(admin.getEmail(), admin.getRole().name());
  }

  @AfterEach
  void tearDown() {
    cleaner.clean();
  }

  @Test
  @DisplayName("PUT /api/theatres/{id} updates theatre successfully")
  void updateTheatre_success() throws Exception {
    // Create first
    TheatreRequest create = new TheatreRequest();
    create.setName("Old Name");
    create.setAddress("Old Addr");
    create.setCity("OldCity");
    String body =
        mockMvc
            .perform(
                post("/api/theatres")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(create)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long id = objectMapper.readValue(body, TheatreResponse.class).getId();

    // Update
    TheatreRequest update = new TheatreRequest();
    update.setName("New Name");
    update.setAddress("New Addr");
    update.setCity("NewCity");
    mockMvc
        .perform(
            put("/api/theatres/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("New Name")))
        .andExpect(jsonPath("$.city", is("NewCity")));
  }

  @Test
  @DisplayName("DELETE /api/theatres/{id} deletes theatre successfully")
  void deleteTheatre_success() throws Exception {
    TheatreRequest create = new TheatreRequest();
    create.setName("To Delete");
    create.setAddress("Addr");
    create.setCity("City");
    String body =
        mockMvc
            .perform(
                post("/api/theatres")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(create)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long id = objectMapper.readValue(body, TheatreResponse.class).getId();

    mockMvc
        .perform(delete("/api/theatres/" + id).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/theatres/" + id)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/theatres?city= filters by city")
  void getTheatresByCity() throws Exception {
    TheatreRequest t1 = new TheatreRequest();
    t1.setName("PVR");
    t1.setAddress("Addr1");
    t1.setCity("Bangalore");
    TheatreRequest t2 = new TheatreRequest();
    t2.setName("INOX");
    t2.setAddress("Addr2");
    t2.setCity("Mumbai");

    for (TheatreRequest t : new TheatreRequest[] {t1, t2}) {
      mockMvc
          .perform(
              post("/api/theatres")
                  .header("Authorization", "Bearer " + adminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(t)))
          .andExpect(status().isCreated());
    }

    mockMvc
        .perform(get("/api/theatres?city=Bangalore"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("PVR")));
  }

  @Test
  @DisplayName("PUT /api/theatres/{id} returns 404 for missing theatre")
  void updateTheatre_notFound() throws Exception {
    TheatreRequest req = new TheatreRequest();
    req.setName("X");
    req.setAddress("Y");
    req.setCity("Z");
    mockMvc
        .perform(
            put("/api/theatres/99999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/theatres with missing fields returns 400 with validation errors")
  void createTheatre_validationFails() throws Exception {
    mockMvc
        .perform(
            post("/api/theatres")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors", notNullValue()));
  }
}
