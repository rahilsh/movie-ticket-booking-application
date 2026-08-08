package com.rsh.mtba.repository;

import com.rsh.mtba.entity.*;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.rsh.mtba.support.PostgreSqlIntegrationTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the JdbcTemplate repository layer.
 * Covers insert, update, findById (found + not found), deleteAll,
 * and the optimistic lock failure path in ShowSeatRepository.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class PostgreSqlRepositoryIT extends PostgreSqlIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private TheatreRepository theatreRepository;
  @Autowired private ScreenRepository screenRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowSeatRepository showSeatRepository;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private PaymentRepository paymentRepository;

  // ── UserRepository ───────────────────────────────────────────────────

  @Test @DisplayName("UserRepository: insert, findById, update, findByEmail, existsByEmail")
  void user_crudAndLookups() {
    User u = User.builder().name("Alice").email("alice@repo.test")
        .passwordHash("h").gender(User.Gender.FEMALE).role(User.Role.ROLE_USER).build();
    userRepository.save(u);
    assertThat(u.getId()).isNotNull();

    Optional<User> found = userRepository.findById(u.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("alice@repo.test");

    // update path
    u.setName("Alice Updated");
    userRepository.save(u);
    assertThat(userRepository.findById(u.getId()).get().getName()).isEqualTo("Alice Updated");

    assertThat(userRepository.findByEmail("alice@repo.test")).isPresent();
    assertThat(userRepository.findByEmail("no@one.com")).isEmpty();
    assertThat(userRepository.existsByEmail("alice@repo.test")).isTrue();
    assertThat(userRepository.existsByEmail("no@one.com")).isFalse();

    // findById not found
    assertThat(userRepository.findById(999999L)).isEmpty();
  }

  // ── TheatreRepository ─────────────────────────────────────────────────

  @Test @DisplayName("TheatreRepository: insert, update, findById, findAll, findByCity, delete, not-found")
  void theatre_crud() {
    Theatre t = Theatre.builder().name("PVR").address("MG Road").city("Bangalore").build();
    theatreRepository.save(t);
    assertThat(t.getId()).isNotNull();

    // update
    t.setCity("Mumbai");
    theatreRepository.save(t);
    assertThat(theatreRepository.findById(t.getId()).get().getCity()).isEqualTo("Mumbai");

    assertThat(theatreRepository.findAll()).isNotEmpty();
    assertThat(theatreRepository.findByCity("Mumbai")).hasSize(1);
    assertThat(theatreRepository.findByCity("Nowhere")).isEmpty();
    assertThat(theatreRepository.findById(999999L)).isEmpty();

    theatreRepository.delete(t.getId());
    assertThat(theatreRepository.findById(t.getId())).isEmpty();
  }

  // ── ScreenRepository ──────────────────────────────────────────────────

  @Test @DisplayName("ScreenRepository: insert, update, findById, findByTheatreId, not-found")
  void screen_crud() {
    Theatre t = Theatre.builder().name("T1").address("Addr").city("City").build();
    theatreRepository.save(t);

    Screen s = Screen.builder().name("S1").rows(5).cols(10).totalCapacity(50)
        .theatreId(t.getId()).build();
    screenRepository.save(s);
    assertThat(s.getId()).isNotNull();

    // findById populates theatreName from join
    Screen fetched = screenRepository.findById(s.getId()).orElseThrow();
    assertThat(fetched.getTheatreName()).isEqualTo("T1");

    // update path
    s.setName("S1-Updated");
    screenRepository.save(s);
    assertThat(screenRepository.findById(s.getId()).get().getName()).isEqualTo("S1-Updated");

    assertThat(screenRepository.findByTheatreId(t.getId())).hasSize(1);
    assertThat(screenRepository.findById(999999L)).isEmpty();
  }

  // ── SeatRepository ────────────────────────────────────────────────────

  @Test @DisplayName("SeatRepository: saveAll, findByScreenId, findByScreenIdAndLabel, not-found")
  void seat_crud() {
    Theatre t = Theatre.builder().name("T1").address("A").city("C").build();
    theatreRepository.save(t);
    Screen s = Screen.builder().name("S1").rows(1).cols(2).totalCapacity(2).theatreId(t.getId()).build();
    screenRepository.save(s);

    Seat seat = Seat.builder().label("A1").rowNumber(0).colNumber(0)
        .type(Seat.SeatType.REGULAR).screenId(s.getId()).build();
    seatRepository.saveAll(List.of(seat));
    assertThat(seat.getId()).isNotNull();

    assertThat(seatRepository.findByScreenId(s.getId())).hasSize(1);
    assertThat(seatRepository.findByScreenIdAndLabel(s.getId(), "A1")).isPresent();
    assertThat(seatRepository.findByScreenIdAndLabel(s.getId(), "Z99")).isEmpty();
  }

  // ── ShowRepository ────────────────────────────────────────────────────

  @Test @DisplayName("ShowRepository: insert, update, findById, findByScreenId, findUpcoming, not-found")
  void show_crud() {
    Theatre t = Theatre.builder().name("T1").address("A").city("C").build();
    theatreRepository.save(t);
    Screen sc = Screen.builder().name("S1").rows(5).cols(10).totalCapacity(50).theatreId(t.getId()).build();
    screenRepository.save(sc);

    Show show = Show.builder().movieName("Dune").screenId(sc.getId())
        .startTime(LocalDateTime.now().plusDays(1))
        .endTime(LocalDateTime.now().plusDays(1).plusHours(3))
        .basePriceInPaise(25000).build();
    showRepository.save(show);
    assertThat(show.getId()).isNotNull();

    // findById populates joined fields
    Show fetched = showRepository.findById(show.getId()).orElseThrow();
    assertThat(fetched.getScreenName()).isEqualTo("S1");
    assertThat(fetched.getTheatreName()).isEqualTo("T1");

    // update
    show.setMovieName("Dune: Part 2");
    showRepository.save(show);
    assertThat(showRepository.findById(show.getId()).get().getMovieName()).isEqualTo("Dune: Part 2");

    assertThat(showRepository.findByScreenId(sc.getId())).hasSize(1);
    assertThat(showRepository.findUpcomingShows(LocalDateTime.now())).isNotEmpty();
    assertThat(showRepository.findById(999999L)).isEmpty();
  }

  // ── ShowSeatRepository ────────────────────────────────────────────────

  @Test @DisplayName("ShowSeatRepository: saveAll insert/update, findByShowId, findByStatus, findWithLock, optimistic lock failure")
  void showSeat_crudAndLocking() {
    Theatre t = Theatre.builder().name("T1").address("A").city("C").build();
    theatreRepository.save(t);
    Screen sc = Screen.builder().name("S1").rows(1).cols(2).totalCapacity(2).theatreId(t.getId()).build();
    screenRepository.save(sc);
    Seat seat = Seat.builder().label("A1").rowNumber(0).colNumber(0)
        .type(Seat.SeatType.REGULAR).screenId(sc.getId()).build();
    seatRepository.saveAll(List.of(seat));
    Show show = Show.builder().movieName("M").screenId(sc.getId())
        .startTime(LocalDateTime.now().plusDays(1))
        .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
        .basePriceInPaise(10000).build();
    showRepository.save(show);

    ShowSeat ss = ShowSeat.builder().showId(show.getId()).seatId(seat.getId())
        .seatLabel("A1").status(ShowSeatStatus.AVAILABLE).build();
    showSeatRepository.saveAll(List.of(ss));
    assertThat(ss.getId()).isNotNull();
    assertThat(ss.getVersion()).isEqualTo(0L);

    // update (status change)
    ss.setStatus(ShowSeatStatus.LOCKED);
    showSeatRepository.saveAll(List.of(ss));
    assertThat(ss.getVersion()).isEqualTo(1L);

    assertThat(showSeatRepository.findByShowId(show.getId())).hasSize(1);
    assertThat(showSeatRepository.findByShowIdAndStatus(show.getId(), ShowSeatStatus.LOCKED)).hasSize(1);
    assertThat(showSeatRepository.findByShowIdAndStatus(show.getId(), ShowSeatStatus.AVAILABLE)).isEmpty();

    List<ShowSeat> locked = showSeatRepository.findByShowIdAndSeatLabelInWithLock(
        show.getId(), List.of("A1"));
    assertThat(locked).hasSize(1);

    // Optimistic lock failure: simulate stale version
    ss.setVersion(0L); // stale
    ss.setStatus(ShowSeatStatus.BOOKED);
    assertThatThrownBy(() -> showSeatRepository.saveAll(List.of(ss)))
        .isInstanceOf(OptimisticLockingFailureException.class);

    // Empty lock query
    assertThat(showSeatRepository.findByShowIdAndSeatLabelInWithLock(show.getId(), List.of())).isEmpty();
  }

  // ── BookingRepository ─────────────────────────────────────────────────

  @Test @DisplayName("BookingRepository: insert, update, findById, findByUserId, not-found")
  void booking_crud() {
    Theatre t = Theatre.builder().name("T1").address("A").city("C").build();
    theatreRepository.save(t);
    Screen sc = Screen.builder().name("S1").rows(1).cols(2).totalCapacity(2).theatreId(t.getId()).build();
    screenRepository.save(sc);
    Show show = Show.builder().movieName("M").screenId(sc.getId())
        .startTime(LocalDateTime.now().plusDays(1))
        .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
        .basePriceInPaise(10000).build();
    showRepository.save(show);
    User u = User.builder().name("Bob").email("bob@repo.test").passwordHash("h")
        .gender(User.Gender.MALE).role(User.Role.ROLE_USER).build();
    userRepository.save(u);

    Booking b = Booking.builder().userId(u.getId()).showId(show.getId())
        .totalAmountInPaise(10000).status(BookingStatus.PROCESSING).build();
    bookingRepository.save(b);
    assertThat(b.getId()).isNotNull();

    // findById populates movieName from join
    Booking fetched = bookingRepository.findById(b.getId()).orElseThrow();
    assertThat(fetched.getMovieName()).isEqualTo("M");

    // update
    b.setStatus(BookingStatus.PAYMENT_INITIATED);
    bookingRepository.save(b);
    assertThat(bookingRepository.findById(b.getId()).get().getStatus())
        .isEqualTo(BookingStatus.PAYMENT_INITIATED);

    assertThat(bookingRepository.findByUserId(u.getId())).hasSize(1);
    assertThat(bookingRepository.findById(999999L)).isEmpty();
  }

  // ── PaymentRepository ─────────────────────────────────────────────────

  @Test @DisplayName("PaymentRepository: insert, update, findById, findByTransactionId, findByBookingId, not-found")
  void payment_crud() {
    Theatre t = Theatre.builder().name("T1").address("A").city("C").build();
    theatreRepository.save(t);
    Screen sc = Screen.builder().name("S1").rows(1).cols(2).totalCapacity(2).theatreId(t.getId()).build();
    screenRepository.save(sc);
    Show show = Show.builder().movieName("M").screenId(sc.getId())
        .startTime(LocalDateTime.now().plusDays(1))
        .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
        .basePriceInPaise(10000).build();
    showRepository.save(show);
    User u = User.builder().name("Carol").email("carol@repo.test").passwordHash("h")
        .gender(User.Gender.FEMALE).role(User.Role.ROLE_USER).build();
    userRepository.save(u);
    Booking b = Booking.builder().userId(u.getId()).showId(show.getId())
        .totalAmountInPaise(10000).status(BookingStatus.PAYMENT_INITIATED).build();
    bookingRepository.save(b);

    Payment p = Payment.builder().transactionId("TXN-REPO-001").bookingId(b.getId())
        .amountInPaise(10000).status(PaymentStatus.INITIATED).build();
    paymentRepository.save(p);
    assertThat(p.getId()).isNotNull();

    // update
    p.setStatus(PaymentStatus.COMPLETED);
    p.setCompletedAt(LocalDateTime.now());
    paymentRepository.save(p);
    assertThat(paymentRepository.findById(p.getId()).get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);

    assertThat(paymentRepository.findByTransactionId("TXN-REPO-001")).isPresent();
    assertThat(paymentRepository.findByTransactionId("NOPE")).isEmpty();
    assertThat(paymentRepository.findByBookingId(b.getId())).isPresent();
    assertThat(paymentRepository.findByBookingId(999999L)).isEmpty();
    assertThat(paymentRepository.findById(999999L)).isEmpty();

    // failure_reason update path
    p.setStatus(PaymentStatus.FAILED);
    p.setFailureReason("Card declined");
    p.setCompletedAt(null);
    paymentRepository.save(p);
    assertThat(paymentRepository.findById(p.getId()).get().getFailureReason()).isEqualTo("Card declined");
  }
}
