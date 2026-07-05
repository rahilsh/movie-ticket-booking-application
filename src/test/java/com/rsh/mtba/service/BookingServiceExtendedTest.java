package com.rsh.mtba.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.entity.*;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Screen;
import com.rsh.mtba.entity.Seat;
import com.rsh.mtba.entity.Show;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.entity.Theatre;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceExtendedTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private ShowSeatRepository showSeatRepository;
  @Mock private ShowService showService;
  @Mock private UserService userService;

  @InjectMocks private BookingService bookingService;

  private User user;
  private Show show;
  private ShowSeat ss1;
  private Booking booking;

  @BeforeEach
  void setUp() {
    Theatre theatre = Theatre.builder().id(1L).name("PVR").address("addr").city("City").build();
    Screen screen =
        Screen.builder()
            .id(1L)
            .name("S1")
            .rows(5)
            .cols(10)
            .totalCapacity(50)
            .theatre(theatre)
            .build();
    show =
        Show.builder()
            .id(1L)
            .movieName("Movie")
            .screen(screen)
            .startTime(LocalDateTime.now().plusHours(2))
            .endTime(LocalDateTime.now().plusHours(5))
            .basePriceInPaise(25000)
            .build();
    user =
        User.builder()
            .id(1L)
            .name("Alice")
            .email("alice@test.com")
            .passwordHash("h")
            .gender(User.Gender.FEMALE)
            .role(User.Role.ROLE_USER)
            .build();
    Seat seat =
        Seat.builder()
            .id(1L)
            .label("A1")
            .rowNumber(0)
            .colNumber(0)
            .type(Seat.SeatType.REGULAR)
            .screen(screen)
            .build();
    ss1 =
        ShowSeat.builder()
            .id(1L)
            .show(show)
            .seat(seat)
            .status(ShowSeatStatus.LOCKED)
            .version(0L)
            .build();
    booking =
        Booking.builder()
            .id(10L)
            .user(user)
            .show(show)
            .showSeats(List.of(ss1))
            .totalAmountInPaise(25000)
            .status(BookingStatus.PAYMENT_INITIATED)
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("confirmBooking() transitions booking to COMPLETED and seats to BOOKED")
  void confirmBooking_success() {
    when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
    when(bookingRepository.save(any())).thenReturn(booking);

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

    assertThatThrownBy(() -> bookingService.getById(999L))
        .isInstanceOf(ResourceNotFoundException.class);
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
