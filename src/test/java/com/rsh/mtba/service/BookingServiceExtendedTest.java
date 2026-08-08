package com.rsh.mtba.service;

import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import org.springframework.security.access.AccessDeniedException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceExtendedTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private ShowSeatRepository showSeatRepository;
  @Mock private ShowService showService;
  @Mock private UserService userService;

  @InjectMocks private BookingService bookingService;

  private ShowSeat ss1;
  private Booking booking;
  private User owner;

  @BeforeEach
  void setUp() {
    ss1 = ShowSeat.builder().id(1L).showId(1L).seatId(1L).seatLabel("A1")
        .status(ShowSeatStatus.LOCKED).version(0L).build();
    booking = Booking.builder()
        .id(10L).userId(1L).showId(1L).movieName("Movie")
        .seatLabels(List.of("A1")).totalAmountInPaise(25000)
        .status(BookingStatus.PAYMENT_INITIATED).createdAt(LocalDateTime.now()).build();
    owner = User.builder().id(1L).role(User.Role.ROLE_USER).build();
  }

  @Test
  @DisplayName("confirmBooking() transitions booking to COMPLETED and seats to BOOKED")
  void confirmBooking_success() {
    when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
    when(bookingRepository.save(any())).thenReturn(booking);
    when(showSeatRepository.findByShowId(1L)).thenReturn(List.of(ss1));

    BookingResponse resp = bookingService.confirmBooking(10L);

    assertThat(resp.getStatus()).isEqualTo("COMPLETED");
    assertThat(ss1.getStatus()).isEqualTo(ShowSeatStatus.BOOKED);
  }

  @Test
  @DisplayName("confirmBooking() throws BookingException when not in PAYMENT_INITIATED state")
  void confirmBooking_wrongState_throws() {
    booking.setStatus(BookingStatus.PROCESSING);
    when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.confirmBooking(10L))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("PAYMENT_INITIATED");
  }

  @Test
  @DisplayName("getByUser() returns all bookings for the user")
  void getByUser_success() {
    when(bookingRepository.findByUserId(1L)).thenReturn(List.of(booking));

    List<BookingResponse> result = bookingService.getByUser(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("getById() throws ResourceNotFoundException for unknown booking")
  void getById_notFound() {
    when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bookingService.getById(999L, owner))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getById() allows the owner and administrators")
  void getById_authorized() {
    when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

    assertThat(bookingService.getById(10L, owner).getId()).isEqualTo(10L);
    assertThat(bookingService.getById(
        10L, User.builder().id(2L).role(User.Role.ROLE_ADMIN).build()).getId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("getById() denies unrelated users")
  void getById_forbidden() {
    when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.getById(
        10L, User.builder().id(2L).role(User.Role.ROLE_USER).build()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("cancelBooking() throws BookingException when already cancelled")
  void cancelBooking_alreadyCancelled_throws() {
    booking.setStatus(BookingStatus.CANCELLED);
    when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.cancelBooking(10L, 1L))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("already cancelled");
  }
}
