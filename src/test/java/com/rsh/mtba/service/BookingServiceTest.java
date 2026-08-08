package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.BookingRequest;
import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Show;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.SeatNotAvailableException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private ShowSeatRepository showSeatRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ShowService showService;
  @Mock private UserService userService;

  @InjectMocks private BookingService bookingService;

  private User user;
  private Show show;
  private ShowSeat showSeat1, showSeat2;

  @BeforeEach
  void setUp() {
    user = User.builder().id(1L).name("Alice").email("alice@test.com")
        .passwordHash("hash").gender(User.Gender.FEMALE).role(User.Role.ROLE_USER).build();
    show = Show.builder().id(1L).movieName("Inception").screenId(1L)
        .startTime(LocalDateTime.now().plusHours(2))
        .endTime(LocalDateTime.now().plusHours(5))
        .basePriceInPaise(25000).build();
    showSeat1 = ShowSeat.builder().id(1L).showId(1L).seatId(1L).seatLabel("A1")
        .rowNumber(0).colNumber(0).status(ShowSeatStatus.AVAILABLE).version(0L).build();
    showSeat2 = ShowSeat.builder().id(2L).showId(1L).seatId(2L).seatLabel("A2")
        .rowNumber(0).colNumber(1).status(ShowSeatStatus.AVAILABLE).version(0L).build();
  }

  @Test
  @DisplayName("book() creates booking and locks seats when all seats are available")
  void book_success() {
    BookingRequest request = new BookingRequest();
    request.setShowId(1L);
    request.setSeatLabels(List.of("A1", "A2"));

    when(userService.findById(1L)).thenReturn(user);
    when(showService.findById(1L)).thenReturn(show);
    when(showSeatRepository.findByShowIdAndSeatLabelInWithLock(1L, List.of("A1", "A2")))
        .thenReturn(List.of(showSeat1, showSeat2));

    Booking savedBooking = Booking.builder()
        .id(100L).userId(1L).showId(1L).movieName("Inception")
        .totalAmountInPaise(50000).status(BookingStatus.PROCESSING)
        .seatLabels(List.of("A1", "A2")).createdAt(LocalDateTime.now()).build();
    when(bookingRepository.save(any())).thenReturn(savedBooking);

    BookingResponse response = bookingService.book(1L, request);

    assertThat(response.getId()).isEqualTo(100L);
    assertThat(response.getTotalAmountInPaise()).isEqualTo(50000);
    assertThat(response.getStatus()).isEqualTo("PROCESSING");
    assertThat(showSeat1.getStatus()).isEqualTo(ShowSeatStatus.LOCKED);
    assertThat(showSeat2.getStatus()).isEqualTo(ShowSeatStatus.LOCKED);
    verify(showSeatRepository, times(2)).saveAll(anyList());
    verify(bookingRepository).saveBookingSeats(eq(100L), anyList());
  }

  @Test
  @DisplayName("book() throws SeatNotAvailableException when a seat is already LOCKED")
  void book_seatAlreadyLocked_throws() {
    showSeat1.setStatus(ShowSeatStatus.LOCKED);
    BookingRequest request = new BookingRequest();
    request.setShowId(1L);
    request.setSeatLabels(List.of("A1", "A2"));

    when(userService.findById(1L)).thenReturn(user);
    when(showService.findById(1L)).thenReturn(show);
    when(showSeatRepository.findByShowIdAndSeatLabelInWithLock(1L, List.of("A1", "A2")))
        .thenReturn(List.of(showSeat1, showSeat2));

    assertThatThrownBy(() -> bookingService.book(1L, request))
        .isInstanceOf(SeatNotAvailableException.class)
        .hasMessageContaining("A1");
  }

  @Test
  @DisplayName("cancelBooking() releases seats and sets status to CANCELLED")
  void cancelBooking_success() {
    Booking booking = Booking.builder()
        .id(100L).userId(1L).showId(1L).movieName("Movie")
        .seatLabels(List.of("A1", "A2")).totalAmountInPaise(50000)
        .status(BookingStatus.PROCESSING).createdAt(LocalDateTime.now()).build();

    when(bookingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(booking));
    when(bookingRepository.save(any())).thenReturn(booking);

    BookingResponse response = bookingService.cancelBooking(100L, 1L);

    assertThat(response.getStatus()).isEqualTo("CANCELLED");
    verify(showSeatRepository).releaseOwnedSeats(100L);
  }

  @Test
  @DisplayName("cancelBooking() throws BookingException when booking is COMPLETED")
  void cancelBooking_completed_throws() {
    Booking booking = Booking.builder()
        .id(100L).userId(1L).showId(1L).movieName("Movie").seatLabels(List.of())
        .totalAmountInPaise(50000).status(BookingStatus.COMPLETED).createdAt(LocalDateTime.now()).build();
    when(bookingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.cancelBooking(100L, 1L))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("completed");
  }

  @Test
  @DisplayName("cancelBooking() throws BookingException when user doesn't own the booking")
  void cancelBooking_wrongUser_throws() {
    Booking booking = Booking.builder()
        .id(100L).userId(99L).showId(1L).movieName("Movie").seatLabels(List.of())
        .totalAmountInPaise(50000).status(BookingStatus.PROCESSING).createdAt(LocalDateTime.now()).build();
    when(bookingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.cancelBooking(100L, 1L))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("own");
  }
}
