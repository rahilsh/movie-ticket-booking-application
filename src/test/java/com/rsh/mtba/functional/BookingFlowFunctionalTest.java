package com.rsh.mtba.functional;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.mtba.dto.request.BookingRequest;
import com.rsh.mtba.dto.request.LoginRequest;
import com.rsh.mtba.dto.request.PaymentRequest;
import com.rsh.mtba.dto.request.RegisterRequest;
import com.rsh.mtba.dto.request.ScreenRequest;
import com.rsh.mtba.dto.request.ShowRequest;
import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.ApiError;
import com.rsh.mtba.dto.response.AuthResponse;
import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.dto.response.ScreenResponse;
import com.rsh.mtba.dto.response.ShowResponse;
import com.rsh.mtba.dto.response.ShowSeatResponse;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.dto.response.UserResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.entity.User.Gender;
import com.rsh.mtba.entity.User.Role;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ScreenRepository;
import com.rsh.mtba.repository.ShowRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import com.rsh.mtba.repository.TheatreRepository;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.util.TestDataCleaner;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Functional end-to-end tests that start the full Spring Boot server on a random
 * port and exercise all API endpoints via real HTTP (TestRestTemplate).
 *
 * Flow: register user → register admin → login both → admin creates theatre/screen/show
 *       → user sees shows/seats → user books → user pays → payment confirmed → booking COMPLETED
 *       → user cancels another booking → seats released back to AVAILABLE
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingFlowFunctionalTest {

    // Shared state across ordered tests within this class
    private String userToken;
    private String adminToken;
    private Long theatreId;
    private Long screenId;
    private Long showId;
    private Long bookingId;
    private Long cancelBookingId;
    private String transactionId;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TestDataCleaner cleaner;

    @BeforeAll
    void setUpAll() {
        cleaner.clean();
    }

    @AfterAll
    void tearDownAll() {
        cleaner.clean();
    }

    // ─────────────────────────── Auth ──────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("1. Register a regular user via POST /api/auth/register")
    void registerUser() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice");
        req.setEmail("alice@test.com");
        req.setPassword("password123");
        req.setGender(Gender.FEMALE);

        ResponseEntity<UserResponse> resp = restTemplate.postForEntity(
            "/api/auth/register", req, UserResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getEmail()).isEqualTo("alice@test.com");
        assertThat(resp.getBody().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @Order(2)
    @DisplayName("2. Register an admin user (directly via DB to avoid chicken-and-egg)")
    void createAdminUser() {
        // Admin provisioning in prod happens out of band; we seed one for tests
        User admin = userRepository.save(
            User.builder()
                .name("Admin")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .gender(Gender.MALE)
                .role(Role.ROLE_ADMIN)
                .build());
        assertThat(admin.getId()).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("3. Login as user via POST /api/auth/login and store JWT")
    void loginUser() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("password123");

        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
            "/api/auth/login", req, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getToken()).isNotBlank();
        userToken = resp.getBody().getToken();
    }

    @Test
    @Order(4)
    @DisplayName("4. Login as admin via POST /api/auth/login and store JWT")
    void loginAdmin() {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("adminpass");

        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
            "/api/auth/login", req, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = resp.getBody().getToken();
    }

    @Test
    @Order(5)
    @DisplayName("5. Register returns 409 on duplicate email")
    void registerDuplicate() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice Again");
        req.setEmail("alice@test.com");
        req.setPassword("password123");
        req.setGender(Gender.FEMALE);

        ResponseEntity<ApiError> resp = restTemplate.postForEntity(
            "/api/auth/register", req, ApiError.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getError()).isEqualTo("Conflict");
    }

    // ─────────────────────────── Admin: Onboarding ─────────────────────

    @Test
    @Order(6)
    @DisplayName("6. Admin creates a theatre via POST /api/theatres")
    void adminCreatesTheatre() {
        TheatreRequest req = new TheatreRequest();
        req.setName("PVR Cinemas");
        req.setAddress("MG Road");
        req.setCity("Bangalore");

        ResponseEntity<TheatreResponse> resp = restTemplate.exchange(
            "/api/theatres", HttpMethod.POST,
            new HttpEntity<>(req, adminHeaders()), TheatreResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getName()).isEqualTo("PVR Cinemas");
        theatreId = resp.getBody().getId();
    }

    @Test
    @Order(7)
    @DisplayName("7. Regular user cannot create a theatre (403)")
    void userCannotCreateTheatre() {
        TheatreRequest req = new TheatreRequest();
        req.setName("Hack Cinema");
        req.setAddress("somewhere");
        req.setCity("Mumbai");

        ResponseEntity<ApiError> resp = restTemplate.exchange(
            "/api/theatres", HttpMethod.POST,
            new HttpEntity<>(req, userHeaders()), ApiError.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(8)
    @DisplayName("8. GET /api/theatres is public and returns created theatre")
    void listTheatresPublic() {
        ResponseEntity<List<TheatreResponse>> resp = restTemplate.exchange(
            "/api/theatres", HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotEmpty();
        assertThat(resp.getBody()).anyMatch(t -> t.getName().equals("PVR Cinemas"));
    }

    @Test
    @Order(9)
    @DisplayName("9. Admin adds a screen to the theatre via POST /api/theatres/{id}/screens")
    void adminAddsScreen() {
        ScreenRequest req = new ScreenRequest();
        req.setName("Screen 1");
        req.setRows(5);
        req.setCols(10);

        ResponseEntity<ScreenResponse> resp = restTemplate.exchange(
            "/api/theatres/" + theatreId + "/screens", HttpMethod.POST,
            new HttpEntity<>(req, adminHeaders()), ScreenResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getTotalCapacity()).isEqualTo(50);
        screenId = resp.getBody().getId();
    }

    @Test
    @Order(10)
    @DisplayName("10. Admin schedules a show via POST /api/screens/{id}/shows")
    void adminSchedulesShow() {
        ShowRequest req = new ShowRequest();
        req.setMovieName("Inception");
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        req.setBasePriceInPaise(25000);

        ResponseEntity<ShowResponse> resp = restTemplate.exchange(
            "/api/screens/" + screenId + "/shows", HttpMethod.POST,
            new HttpEntity<>(req, adminHeaders()), ShowResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getMovieName()).isEqualTo("Inception");
        assertThat(resp.getBody().getAvailableSeats()).isEqualTo(50);
        showId = resp.getBody().getId();
    }

    // ─────────────────────────── User: Browse ──────────────────────────

    @Test
    @Order(11)
    @DisplayName("11. GET /api/shows (upcoming) returns the scheduled show")
    void getUpcomingShows() {
        ResponseEntity<List<ShowResponse>> resp = restTemplate.exchange(
            "/api/shows", HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).anyMatch(s -> s.getMovieName().equals("Inception"));
    }

    @Test
    @Order(12)
    @DisplayName("12. GET /api/shows/{id}/seats returns 50 AVAILABLE seats")
    void getShowSeats() {
        ResponseEntity<List<ShowSeatResponse>> resp = restTemplate.exchange(
            "/api/shows/" + showId + "/seats", HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(50);
        assertThat(resp.getBody()).allMatch(s -> s.getStatus().equals("AVAILABLE"));
    }

    // ─────────────────────────── User: Book ────────────────────────────

    @Test
    @Order(13)
    @DisplayName("13. User books seats A1 and A2 via POST /api/bookings")
    void userBooksSeats() {
        BookingRequest req = new BookingRequest();
        req.setShowId(showId);
        req.setSeatLabels(List.of("A1", "A2"));

        ResponseEntity<BookingResponse> resp = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST,
            new HttpEntity<>(req, userHeaders()), BookingResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getStatus()).isEqualTo("PROCESSING");
        assertThat(resp.getBody().getTotalAmountInPaise()).isEqualTo(50000); // 2 × 25000
        assertThat(resp.getBody().getSeatLabels()).containsExactlyInAnyOrder("A1", "A2");
        bookingId = resp.getBody().getId();
    }

    @Test
    @Order(14)
    @DisplayName("14. Seats A1/A2 are now LOCKED — another user cannot book the same seats")
    void doubleBookingPrevented() {
        BookingRequest req = new BookingRequest();
        req.setShowId(showId);
        req.setSeatLabels(List.of("A1")); // already LOCKED

        ResponseEntity<ApiError> resp = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST,
            new HttpEntity<>(req, userHeaders()), ApiError.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getError()).isEqualTo("Seat Not Available");
    }

    @Test
    @Order(15)
    @DisplayName("15. GET /api/bookings/my returns the user's booking")
    void getMyBookings() {
        ResponseEntity<List<BookingResponse>> resp = restTemplate.exchange(
            "/api/bookings/my", HttpMethod.GET,
            new HttpEntity<>(userHeaders()),
            new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getId()).isEqualTo(bookingId);
    }

    // ─────────────────────────── User: Pay ─────────────────────────────

    @Test
    @Order(16)
    @DisplayName("16. User initiates payment via POST /api/payments")
    void userInitiatesPayment() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(bookingId);
        req.setTransactionId("TXN-FUNCTIONAL-TEST-001");

        ResponseEntity<PaymentResponse> resp = restTemplate.exchange(
            "/api/payments", HttpMethod.POST,
            new HttpEntity<>(req, userHeaders()), PaymentResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getStatus()).isEqualTo("INITIATED");
        assertThat(resp.getBody().getAmountInPaise()).isEqualTo(50000);
        transactionId = resp.getBody().getTransactionId();
    }

    @Test
    @Order(17)
    @DisplayName("17. Booking status is PAYMENT_INITIATED after initiating payment")
    void bookingInPaymentInitiatedState() {
        ResponseEntity<BookingResponse> resp = restTemplate.exchange(
            "/api/bookings/" + bookingId, HttpMethod.GET,
            new HttpEntity<>(userHeaders()), BookingResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getStatus()).isEqualTo("PAYMENT_INITIATED");
    }

    @Test
    @Order(18)
    @DisplayName("18. Payment gateway confirms — POST /api/payments/confirm/{txnId}")
    void gatewayConfirmsPayment() {
        ResponseEntity<PaymentResponse> resp = restTemplate.exchange(
            "/api/payments/confirm/" + transactionId, HttpMethod.POST,
            new HttpEntity<>(adminHeaders()), PaymentResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @Order(19)
    @DisplayName("19. Booking is COMPLETED and seats are BOOKED after confirmed payment")
    void bookingCompletedSeatsBooked() {
        // Check booking
        ResponseEntity<BookingResponse> bookResp = restTemplate.exchange(
            "/api/bookings/" + bookingId, HttpMethod.GET,
            new HttpEntity<>(userHeaders()), BookingResponse.class);
        assertThat(bookResp.getBody().getStatus()).isEqualTo("COMPLETED");

        // Check seats — A1 and A2 should be BOOKED, rest still AVAILABLE
        ResponseEntity<List<ShowSeatResponse>> seatsResp = restTemplate.exchange(
            "/api/shows/" + showId + "/seats", HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {});

        long booked = seatsResp.getBody().stream()
            .filter(s -> s.getStatus().equals("BOOKED")).count();
        long available = seatsResp.getBody().stream()
            .filter(s -> s.getStatus().equals("AVAILABLE")).count();

        assertThat(booked).isEqualTo(2);
        assertThat(available).isEqualTo(48);
    }

    // ─────────────────────────── Cancellation flow ─────────────────────

    @Test
    @Order(20)
    @DisplayName("20. User books seats B1/B2 for a cancellation test")
    void userBooksForCancellation() {
        BookingRequest req = new BookingRequest();
        req.setShowId(showId);
        req.setSeatLabels(List.of("B1", "B2"));

        ResponseEntity<BookingResponse> resp = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST,
            new HttpEntity<>(req, userHeaders()), BookingResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        cancelBookingId = resp.getBody().getId();
    }

    @Test
    @Order(21)
    @DisplayName("21. User cancels the booking — seats B1/B2 return to AVAILABLE")
    void userCancelsBooking() {
        ResponseEntity<BookingResponse> resp = restTemplate.exchange(
            "/api/bookings/" + cancelBookingId, HttpMethod.DELETE,
            new HttpEntity<>(userHeaders()), BookingResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getStatus()).isEqualTo("CANCELLED");

        // Seats B1/B2 should be AVAILABLE again
        ResponseEntity<List<ShowSeatResponse>> seatsResp = restTemplate.exchange(
            "/api/shows/" + showId + "/seats", HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {});

        long available = seatsResp.getBody().stream()
            .filter(s -> s.getStatus().equals("AVAILABLE")).count();
        assertThat(available).isEqualTo(48); // A1/A2 BOOKED, B1/B2 back to AVAILABLE
    }

    // ─────────────────────────── Error cases ───────────────────────────

    @Test
    @Order(22)
    @DisplayName("22. GET /api/theatres/9999 returns 404")
    void nonExistentTheatreReturns404() {
        ResponseEntity<ApiError> resp = restTemplate.getForEntity(
            "/api/theatres/9999", ApiError.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().getError()).isEqualTo("Not Found");
    }

    @Test
    @Order(23)
    @DisplayName("23. POST /api/bookings without auth returns 403")
    void bookingWithoutAuthReturns403() {
        BookingRequest req = new BookingRequest();
        req.setShowId(showId);
        req.setSeatLabels(List.of("C1"));

        ResponseEntity<ApiError> resp = restTemplate.postForEntity(
            "/api/bookings", req, ApiError.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(24)
    @DisplayName("24. GET /api/users/me returns the authenticated user profile")
    void getMeReturnsProfile() {
        ResponseEntity<UserResponse> resp = restTemplate.exchange(
            "/api/users/me", HttpMethod.GET,
            new HttpEntity<>(userHeaders()), UserResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @Order(25)
    @DisplayName("25. GET /api/payments/booking/{id} returns payment details")
    void getPaymentByBooking() {
        ResponseEntity<PaymentResponse> resp = restTemplate.exchange(
            "/api/payments/booking/" + bookingId, HttpMethod.GET,
            new HttpEntity<>(userHeaders()), PaymentResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getStatus()).isEqualTo("COMPLETED");
        assertThat(resp.getBody().getTransactionId()).isEqualTo("TXN-FUNCTIONAL-TEST-001");
    }

    @Test
    @Order(26)
    @DisplayName("26. GET /api/screens/{theatreId}/screens returns the screen")
    void getScreensByTheatre() {
        ResponseEntity<List<ScreenResponse>> resp = restTemplate.exchange(
            "/api/theatres/" + theatreId + "/screens", HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getName()).isEqualTo("Screen 1");
    }

    // ─────────────────────────── Helpers ───────────────────────────────

    private HttpHeaders userHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(userToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(adminToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
