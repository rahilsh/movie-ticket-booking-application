package com.rsh.mtba.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rsh.mtba.dto.request.PaymentRequest;
import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.entity.*;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.entity.Screen;
import com.rsh.mtba.entity.Show;
import com.rsh.mtba.entity.Theatre;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.PaymentException;
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
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private BookingService bookingService;

  @InjectMocks private PaymentService paymentService;

  private User user;
  private Booking booking;

  @BeforeEach
  void setUp() {
    user =
        User.builder()
            .id(1L)
            .name("Alice")
            .email("alice@example.com")
            .passwordHash("hash")
            .gender(User.Gender.FEMALE)
            .role(User.Role.ROLE_USER)
            .build();

    Theatre theatre =
        Theatre.builder().id(1L).name("PVR").address("MG Road").city("Bangalore").build();
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
            .movieName("Inception")
            .startTime(LocalDateTime.now().plusHours(2))
            .endTime(LocalDateTime.now().plusHours(5))
            .basePriceInPaise(25000)
            .screen(screen)
            .build();

    booking =
        Booking.builder()
            .id(10L)
            .user(user)
            .show(show)
            .showSeats(List.of())
            .totalAmountInPaise(25000)
            .status(BookingStatus.PROCESSING)
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("initiatePayment() creates payment and transitions booking to PAYMENT_INITIATED")
  void initiatePayment_success() {
    PaymentRequest request = new PaymentRequest();
    request.setBookingId(10L);
    request.setTransactionId("TXN-CUSTOM-123");

    when(bookingService.findById(10L)).thenReturn(booking);

    Payment savedPayment =
        Payment.builder()
            .id(1L)
            .transactionId("TXN-CUSTOM-123")
            .booking(booking)
            .amountInPaise(25000)
            .status(PaymentStatus.INITIATED)
            .createdAt(LocalDateTime.now())
            .build();
    when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
    when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

    PaymentResponse response = paymentService.initiatePayment(1L, request);

    assertThat(response.getTransactionId()).isEqualTo("TXN-CUSTOM-123");
    assertThat(response.getStatus()).isEqualTo("INITIATED");
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_INITIATED);
  }

  @Test
  @DisplayName("initiatePayment() throws BookingException when user does not own booking")
  void initiatePayment_wrongUser_throws() {
    PaymentRequest request = new PaymentRequest();
    request.setBookingId(10L);

    when(bookingService.findById(10L)).thenReturn(booking);

    assertThatThrownBy(() -> paymentService.initiatePayment(99L, request))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("own");
  }

  @Test
  @DisplayName("initiatePayment() throws BookingException when booking not in PROCESSING state")
  void initiatePayment_notProcessing_throws() {
    booking.setStatus(BookingStatus.COMPLETED);

    PaymentRequest request = new PaymentRequest();
    request.setBookingId(10L);

    when(bookingService.findById(10L)).thenReturn(booking);

    assertThatThrownBy(() -> paymentService.initiatePayment(1L, request))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("PROCESSING");
  }

  @Test
  @DisplayName("confirmPayment() marks payment COMPLETED and confirms booking")
  void confirmPayment_success() {
    booking.setStatus(BookingStatus.PAYMENT_INITIATED);
    Payment payment =
        Payment.builder()
            .id(1L)
            .transactionId("TXN-ABC")
            .booking(booking)
            .amountInPaise(25000)
            .status(PaymentStatus.INITIATED)
            .createdAt(LocalDateTime.now())
            .build();

    when(paymentRepository.findByTransactionId("TXN-ABC")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

    PaymentResponse response = paymentService.confirmPayment("TXN-ABC");

    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    verify(bookingService).confirmBooking(10L);
  }

  @Test
  @DisplayName("confirmPayment() throws PaymentException if payment already failed")
  void confirmPayment_alreadyFailed_throws() {
    Payment payment =
        Payment.builder()
            .id(1L)
            .transactionId("TXN-ABC")
            .booking(booking)
            .amountInPaise(25000)
            .status(PaymentStatus.FAILED)
            .createdAt(LocalDateTime.now())
            .build();

    when(paymentRepository.findByTransactionId("TXN-ABC")).thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> paymentService.confirmPayment("TXN-ABC"))
        .isInstanceOf(PaymentException.class)
        .hasMessageContaining("failed");
  }
}
