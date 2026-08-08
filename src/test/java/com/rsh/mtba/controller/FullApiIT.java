package com.rsh.mtba.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.mtba.dto.request.LoginRequest;
import com.rsh.mtba.dto.request.RegisterRequest;
import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.ApiError;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.security.JwtUtil;
import com.rsh.mtba.support.PostgreSqlIntegrationTest;
import com.rsh.mtba.util.TestDataCleaner;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class FullApiIT extends PostgreSqlIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtUtil jwtUtil;
  @Autowired private TestDataCleaner cleaner;

  private String adminToken;
  private String userToken;

  @BeforeEach
  void setUp() {
    cleaner.clean();
    User admin = userRepository.save(User.builder()
        .name("Admin").email("admin@api.test")
        .passwordHash(passwordEncoder.encode("adminpass"))
        .gender(User.Gender.MALE).role(User.Role.ROLE_ADMIN).build());
    User user = userRepository.save(User.builder()
        .name("User").email("user@api.test")
        .passwordHash(passwordEncoder.encode("userpass"))
        .gender(User.Gender.FEMALE).role(User.Role.ROLE_USER).build());
    adminToken = jwtUtil.generateToken(admin.getEmail());
    userToken = jwtUtil.generateToken(user.getEmail());
  }

  @AfterEach
  void tearDown() {
    cleaner.clean();
  }

  @Test
  void authenticationValidationUsesHttp() {
    RegisterRequest invalid = new RegisterRequest();
    invalid.setName("");
    invalid.setEmail("not-an-email");
    invalid.setPassword("short");

    ResponseEntity<ApiError> register = restTemplate.postForEntity(
        "/api/auth/register", invalid, ApiError.class);
    assertThat(register.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(register.getBody().getValidationErrors()).isNotEmpty();

    LoginRequest login = new LoginRequest();
    login.setEmail("user@api.test");
    login.setPassword("wrong-password");
    ResponseEntity<ApiError> unauthorized = restTemplate.postForEntity(
        "/api/auth/login", login, ApiError.class);
    assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void theatreCrudAndAuthorizationUseHttp() {
    TheatreRequest create = theatre("Initial", "Bangalore");

    ResponseEntity<ApiError> forbidden = restTemplate.exchange(
        "/api/theatres", HttpMethod.POST,
        new HttpEntity<>(create, headers(userToken)), ApiError.class);
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<TheatreResponse> created = restTemplate.exchange(
        "/api/theatres", HttpMethod.POST,
        new HttpEntity<>(create, headers(adminToken)), TheatreResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long id = created.getBody().getId();

    ResponseEntity<TheatreResponse> updated = restTemplate.exchange(
        "/api/theatres/" + id, HttpMethod.PUT,
        new HttpEntity<>(theatre("Updated", "Mumbai"), headers(adminToken)),
        TheatreResponse.class);
    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().getName()).isEqualTo("Updated");

    ResponseEntity<List<TheatreResponse>> filtered = restTemplate.exchange(
        "/api/theatres?city=Mumbai", HttpMethod.GET, HttpEntity.EMPTY,
        new ParameterizedTypeReference<>() {});
    assertThat(filtered.getBody()).extracting(TheatreResponse::getId).containsExactly(id);

    ResponseEntity<Void> deleted = restTemplate.exchange(
        "/api/theatres/" + id, HttpMethod.DELETE,
        new HttpEntity<>(headers(adminToken)), Void.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(restTemplate.getForEntity(
        "/api/theatres/" + id, ApiError.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void protectedEndpointRejectsInvalidTokenOverHttp() {
    ResponseEntity<ApiError> response = restTemplate.exchange(
        "/api/bookings/my", HttpMethod.GET,
        new HttpEntity<>(headers("invalid.token")), ApiError.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  private TheatreRequest theatre(String name, String city) {
    TheatreRequest request = new TheatreRequest();
    request.setName(name);
    request.setAddress("Address");
    request.setCity(city);
    return request;
  }

  private HttpHeaders headers(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
