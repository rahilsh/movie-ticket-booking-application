package com.rsh.mtba.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.rsh.mtba.dto.request.BookingRequest;
import com.rsh.mtba.dto.request.PaymentRequest;
import com.rsh.mtba.dto.request.ScreenRequest;
import com.rsh.mtba.dto.request.ShowRequest;
import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.dto.response.ScreenResponse;
import com.rsh.mtba.dto.response.ShowResponse;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.entity.User.Gender;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ScreenRepository;
import com.rsh.mtba.repository.ShowRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import com.rsh.mtba.repository.TheatreRepository;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.security.JwtUtil;
import com.rsh.mtba.util.TestDataCleaner;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Wide-coverage test exercising controllers/services/exceptions not fully hit by other tests.
 * Covers: ShowController (getByScreen, getUpcoming, getById, getSeats),
 *         ScreenController (getById), UserController (getById admin),
 *         PaymentController (fail, getById, getByBooking),
 *         GlobalExceptionHandler (SeatNotAvailable, BookingException, PaymentException, Duplicate),
 *         JwtAuthFilter (no-auth, invalid token).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullApiCoverageTest {

    private static String adminToken;
    private static String userToken;
    private static Long theatreId;
    private static Long screenId;
    private static Long showId;
    private static Long bookingId;
    private static Long userId;
    private static String txnId;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TestDataCleaner cleaner;

    @BeforeAll
    void setUpAll() {
        // Clean before this class starts so we begin with a guaranteed empty DB
        cleaner.clean();

        User admin = userRepository.save(User.builder()
            .name("Admin").email("cov-admin@test.com")
            .passwordHash(passwordEncoder.encode("adminpass"))
            .gender(Gender.MALE).role(User.Role.ROLE_ADMIN).build());

        User regularUser = userRepository.save(User.builder()
            .name("CovUser").email("cov-user@test.com")
            .passwordHash(passwordEncoder.encode("userpass"))
            .gender(Gender.FEMALE).role(User.Role.ROLE_USER).build());

        userId = regularUser.getId();
        adminToken = jwtUtil.generateToken(admin.getEmail(), admin.getRole().name());
        userToken = jwtUtil.generateToken(regularUser.getEmail(), regularUser.getRole().name());
    }

    @AfterAll
    void tearDownAll() {
        // Clean after this class finishes so no data leaks to subsequent test classes
        cleaner.clean();
    }

    // ────── Setup: create theatre / screen / show ──────

    @Test @Order(1)
    @DisplayName("Admin creates theatre")
    void s1_createTheatre() throws Exception {
        TheatreRequest r = new TheatreRequest();
        r.setName("CovTheatre"); r.setAddress("Addr"); r.setCity("TestCity");
        String body = mockMvc.perform(post("/api/theatres")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        theatreId = objectMapper.readValue(body, TheatreResponse.class).getId();
    }

    @Test @Order(2)
    @DisplayName("Admin creates screen")
    void s2_createScreen() throws Exception {
        ScreenRequest r = new ScreenRequest();
        r.setName("CovScreen"); r.setRows(3); r.setCols(5);
        String body = mockMvc.perform(post("/api/theatres/" + theatreId + "/screens")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        screenId = objectMapper.readValue(body, ScreenResponse.class).getId();
    }

    @Test @Order(3)
    @DisplayName("Admin schedules show")
    void s3_createShow() throws Exception {
        ShowRequest r = new ShowRequest();
        r.setMovieName("CovMovie");
        r.setStartTime(LocalDateTime.now().plusDays(2));
        r.setEndTime(LocalDateTime.now().plusDays(2).plusHours(3));
        r.setBasePriceInPaise(20000);
        String body = mockMvc.perform(post("/api/screens/" + screenId + "/shows")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        showId = objectMapper.readValue(body, ShowResponse.class).getId();
    }

    // ────── ShowController coverage ──────

    @Test @Order(4)
    @DisplayName("GET /api/shows/{id} returns show")
    void getShowById() throws Exception {
        mockMvc.perform(get("/api/shows/" + showId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.movieName", is("CovMovie")))
            .andExpect(jsonPath("$.availableSeats", is(15)));
    }

    @Test @Order(5)
    @DisplayName("GET /api/screens/{id}/shows returns shows for screen")
    void getShowsByScreen() throws Exception {
        mockMvc.perform(get("/api/screens/" + screenId + "/shows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].movieName", is("CovMovie")));
    }

    @Test @Order(6)
    @DisplayName("GET /api/shows returns upcoming shows")
    void getUpcomingShows() throws Exception {
        mockMvc.perform(get("/api/shows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test @Order(7)
    @DisplayName("GET /api/shows/{id}/seats returns seat layout")
    void getSeats() throws Exception {
        mockMvc.perform(get("/api/shows/" + showId + "/seats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(15)))
            .andExpect(jsonPath("$[0].status", is("AVAILABLE")));
    }

    @Test @Order(8)
    @DisplayName("GET /api/shows/9999 returns 404")
    void getShowById_notFound() throws Exception {
        mockMvc.perform(get("/api/shows/9999"))
            .andExpect(status().isNotFound());
    }

    // ────── ScreenController coverage ──────

    @Test @Order(9)
    @DisplayName("GET /api/screens/{id} returns screen")
    void getScreenById() throws Exception {
        mockMvc.perform(get("/api/screens/" + screenId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("CovScreen")));
    }

    // ────── UserController admin coverage ──────

    @Test @Order(10)
    @DisplayName("GET /api/users/{id} as admin returns user")
    void getUserById_asAdmin() throws Exception {
        mockMvc.perform(get("/api/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("cov-user@test.com")));
    }

    @Test @Order(11)
    @DisplayName("GET /api/users/{id} as regular user returns 403")
    void getUserById_asUser_forbidden() throws Exception {
        mockMvc.perform(get("/api/users/" + userId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    // ────── Booking + Payment full cycle ──────

    @Test @Order(12)
    @DisplayName("User books seats")
    void bookSeats() throws Exception {
        BookingRequest r = new BookingRequest();
        r.setShowId(showId);
        r.setSeatLabels(java.util.List.of("A1", "A2"));
        String body = mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        bookingId = objectMapper.readValue(body, BookingResponse.class).getId();
    }

    @Test @Order(13)
    @DisplayName("User initiates payment")
    void initiatePayment() throws Exception {
        PaymentRequest r = new PaymentRequest();
        r.setBookingId(bookingId);
        r.setTransactionId("TXN-COVERAGE-001");
        String body = mockMvc.perform(post("/api/payments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        txnId = objectMapper.readValue(body, PaymentResponse.class).getTransactionId();
    }

    @Test @Order(14)
    @DisplayName("GET /api/payments/{id} returns payment")
    void getPaymentById() throws Exception {
        // First get the payment id via booking
        String body = mockMvc.perform(get("/api/payments/booking/" + bookingId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Long paymentId = objectMapper.readValue(body, PaymentResponse.class).getId();

        mockMvc.perform(get("/api/payments/" + paymentId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId", is("TXN-COVERAGE-001")));
    }

    @Test @Order(15)
    @DisplayName("POST /api/payments/fail/{txnId} marks payment failed and releases seats")
    void failPayment() throws Exception {
        mockMvc.perform(post("/api/payments/fail/" + txnId + "?reason=Declined")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("FAILED")))
            .andExpect(jsonPath("$.failureReason", is("Declined")));
    }

    @Test @Order(16)
    @DisplayName("Trying to pay already-processed booking returns 400")
    void initiatePayment_wrongState() throws Exception {
        // booking is now PAYMENT_FAILED — cannot pay again
        PaymentRequest r = new PaymentRequest();
        r.setBookingId(bookingId);
        r.setTransactionId("TXN-RETRY");
        mockMvc.perform(post("/api/payments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("Booking Error")));
    }

    // ────── GlobalExceptionHandler coverage ──────

    @Test @Order(17)
    @DisplayName("Booking seats that are unavailable returns 409 SeatNotAvailable")
    void seatNotAvailable_handler() throws Exception {
        // Book A1 again (it was released by failPayment, so re-book and attempt double-book)
        BookingRequest r1 = new BookingRequest();
        r1.setShowId(showId);
        r1.setSeatLabels(java.util.List.of("A1"));
        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r1)))
            .andExpect(status().isCreated());

        // Try booking A1 again immediately (now LOCKED)
        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r1)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error", is("Seat Not Available")));
    }

    @Test @Order(18)
    @DisplayName("Confirm non-existent transaction returns 404")
    void confirmPayment_notFound() throws Exception {
        mockMvc.perform(post("/api/payments/confirm/NO-SUCH-TXN")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test @Order(19)
    @DisplayName("Request with invalid JWT returns 403")
    void invalidJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/bookings/my")
                .header("Authorization", "Bearer invalid.token.here"))
            .andExpect(status().isForbidden());
    }

    @Test @Order(20)
    @DisplayName("Request with no JWT to protected endpoint returns 403")
    void noJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/bookings/my"))
            .andExpect(status().isForbidden());
    }

    @Test @Order(21)
    @DisplayName("Fail non-existent transaction returns 404")
    void failPayment_notFound() throws Exception {
        mockMvc.perform(post("/api/payments/fail/GHOST-TXN?reason=x")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test @Order(22)
    @DisplayName("Cancel booking of another user returns 400")
    void cancelOtherUserBooking_returns400() throws Exception {
        // bookingId still belongs to cov-user; try to cancel with admin token (different user)
        // Get a valid booking first: create a new booking from user then try admin cancel
        BookingRequest r = new BookingRequest();
        r.setShowId(showId);
        r.setSeatLabels(java.util.List.of("C1"));
        String body = mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(r)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Long newBookingId = objectMapper.readValue(body, BookingResponse.class).getId();

        mockMvc.perform(delete("/api/bookings/" + newBookingId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("Booking Error")));
    }

    @Test @Order(23)
    @DisplayName("GET /api/theatres?city=TestCity filters correctly")
    void getTheatresByCity() throws Exception {
        mockMvc.perform(get("/api/theatres?city=TestCity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].city", is("TestCity")));
    }
}
