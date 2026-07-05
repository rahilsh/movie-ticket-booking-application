package com.rsh.mtba.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.entity.*;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.entity.Screen;
import com.rsh.mtba.entity.Seat;
import com.rsh.mtba.entity.Show;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.entity.Theatre;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.PaymentException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
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
class PaymentServiceExtendedTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private BookingService bookingService;

  @InjectMocks private PaymentService paymentService;

  private Booking booking;
  private Payment payment;
  private ShowSeat showSeat;

  @BeforeEach
  void setUp() {
    User user =
        User.builder()
            .id(1L)
            .name("Alice")
            .email("alice@test.com")
            .passwordHash("h")
            .gender(User.Gender.FEMALE)
            .role(User.Role.ROLE_USER)
            .build();
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
    Show show =
        Show.builder()
            .id(1L)
            .movieName("Movie")
            .screen(screen)
            .startTime(LocalDateTime.now().plusHours(2))
            .endTime(LocalDateTime.now().plusHours(5))
            .basePriceInPaise(25000)
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
    showSeat =
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
            .showSeats(List.of(showSeat))
            .totalAmountInPaise(25000)
            .status(BookingStatus.PAYMENT_INITIATED)
            .createdAt(LocalDateTime.now())
            .build();
    payment =
        Payment.builder()
            .id(1L)
            .transactionId("TXN-1")
            .booking(booking)
            .amountInPaise(25000)
            .status(PaymentStatus.INITIATED)
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("confirmPayment() is idempotent — returns COMPLETED if already COMPLETED")
  void confirmPayment_idempotent() {
    payment.setStatus(PaymentStatus.COMPLETED);
    when(paymentRepository.findByTransactionId("TXN-1")).thenReturn(Optional.of(payment));

    PaymentResponse resp = paymentService.confirmPayment("TXN-1");

    assertThat(resp.getStatus()).isEqualTo("COMPLETED");
    verify(bookingService, never()).confirmBooking(any());
  }

  @Test
  @DisplayName("failPayment() marks payment FAILED and releases seats")
  void failPayment_success() {
    payment.setStatus(PaymentStatus.INITIATED);
    when(paymentRepository.findByTransactionId("TXN-1")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(bookingRepository.save(any())).thenReturn(booking);

    PaymentResponse resp = paymentService.failPayment("TXN-1", "Insufficient funds");

    assertThat(resp.getStatus()).isEqualTo("FAILED");
    assertThat(resp.getFailureReason()).isEqualTo("Insufficient funds");
    assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
  }

  @Test
  @DisplayName("failPayment() throws ResourceNotFoundException for unknown transactionId")
  void failPayment_notFound() {
    when(paymentRepository.findByTransactionId("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.failPayment("UNKNOWN", "reason"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getById() returns payment DTO")
  void getById_success() {
    when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

    PaymentResponse resp = paymentService.getById(1L);

    assertThat(resp.getTransactionId()).isEqualTo("TXN-1");
    assertThat(resp.getAmountInPaise()).isEqualTo(25000);
  }

  @Test
  @DisplayName("getById() throws ResourceNotFoundException for unknown payment id")
  void getById_notFound() {
    when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.getById(999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getByBookingId() returns payment for booking")
  void getByBookingId_success() {
    when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(payment));

    PaymentResponse resp = paymentService.getByBookingId(10L);

    assertThat(resp.getBookingId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("getByBookingId() throws ResourceNotFoundException when no payment for booking")
  void getByBookingId_notFound() {
    when(paymentRepository.findByBookingId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.getByBookingId(999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("confirmPayment() throws PaymentException if payment already FAILED")
  void confirmPayment_alreadyFailed_throws() {
    payment.setStatus(PaymentStatus.FAILED);
    when(paymentRepository.findByTransactionId("TXN-1")).thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> paymentService.confirmPayment("TXN-1"))
        .isInstanceOf(PaymentException.class);
  }
}
