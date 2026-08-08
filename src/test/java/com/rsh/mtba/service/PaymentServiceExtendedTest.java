package com.rsh.mtba.service;

import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.PaymentException;
import com.rsh.mtba.exception.ResourceNotFoundException;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceExtendedTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private BookingService bookingService;
  @Mock private ShowSeatRepository showSeatRepository;

  @InjectMocks private PaymentService paymentService;

  private Booking booking;
  private Payment payment;
  private ShowSeat showSeat;
  private User owner;

  @BeforeEach
  void setUp() {
    showSeat = ShowSeat.builder().id(1L).showId(1L).seatId(1L).seatLabel("A1")
        .status(ShowSeatStatus.LOCKED).version(0L).build();
    booking = Booking.builder()
        .id(10L).userId(1L).showId(1L).movieName("Movie")
        .seatLabels(List.of("A1")).totalAmountInPaise(25000)
        .status(BookingStatus.PAYMENT_INITIATED).createdAt(LocalDateTime.now()).build();
    payment = Payment.builder()
        .id(1L).transactionId("TXN-1").bookingId(10L)
        .amountInPaise(25000).status(PaymentStatus.INITIATED)
        .createdAt(LocalDateTime.now()).build();
    owner = User.builder().id(1L).role(User.Role.ROLE_USER).build();
  }

  @Test
  @DisplayName("confirmPayment() is idempotent — returns COMPLETED if already COMPLETED")
  void confirmPayment_idempotent() {
    payment.setStatus(PaymentStatus.COMPLETED);
    when(paymentRepository.findByTransactionIdWithLock("TXN-1")).thenReturn(Optional.of(payment));

    PaymentResponse resp = paymentService.confirmPayment("TXN-1");

    assertThat(resp.getStatus()).isEqualTo("COMPLETED");
    verify(bookingService, never()).confirmBooking(any());
  }

  @Test
  @DisplayName("failPayment() marks payment FAILED and releases seats")
  void failPayment_success() {
    when(paymentRepository.findByTransactionIdWithLock("TXN-1")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(bookingService.findById(10L)).thenReturn(booking);
    when(bookingRepository.save(any())).thenReturn(booking);
    when(showSeatRepository.findByShowId(1L)).thenReturn(List.of(showSeat));

    PaymentResponse resp = paymentService.failPayment("TXN-1", "Insufficient funds");

    assertThat(resp.getStatus()).isEqualTo("FAILED");
    assertThat(resp.getFailureReason()).isEqualTo("Insufficient funds");
    assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);
    assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
  }

  @Test
  @DisplayName("failPayment() throws ResourceNotFoundException for unknown transactionId")
  void failPayment_notFound() {
    when(paymentRepository.findByTransactionIdWithLock("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.failPayment("UNKNOWN", "reason"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getById() returns payment DTO")
  void getById_success() {
    when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

    when(bookingService.findById(10L)).thenReturn(booking);

    PaymentResponse resp = paymentService.getById(1L, owner);

    assertThat(resp.getTransactionId()).isEqualTo("TXN-1");
    assertThat(resp.getAmountInPaise()).isEqualTo(25000);
  }

  @Test
  @DisplayName("getById() throws ResourceNotFoundException for unknown payment id")
  void getById_notFound() {
    when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.getById(999L, owner))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getByBookingId() returns payment for booking")
  void getByBookingId_success() {
    when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.of(payment));
    when(bookingService.findById(10L)).thenReturn(booking);

    PaymentResponse resp = paymentService.getByBookingId(10L, owner);

    assertThat(resp.getBookingId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("getByBookingId() throws ResourceNotFoundException when no payment for booking")
  void getByBookingId_notFound() {
    when(bookingService.findById(999L)).thenReturn(
        Booking.builder().id(999L).userId(1L).build());
    when(paymentRepository.findByBookingId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.getByBookingId(999L, owner))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("confirmPayment() throws PaymentException if payment already FAILED")
  void confirmPayment_alreadyFailed_throws() {
    payment.setStatus(PaymentStatus.FAILED);
    when(paymentRepository.findByTransactionIdWithLock("TXN-1")).thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> paymentService.confirmPayment("TXN-1"))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  @DisplayName("payment reads deny unrelated users and allow administrators")
  void paymentRead_authorization() {
    when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
    when(bookingService.findById(10L)).thenReturn(booking);
    User other = User.builder().id(2L).role(User.Role.ROLE_USER).build();
    User admin = User.builder().id(3L).role(User.Role.ROLE_ADMIN).build();

    assertThatThrownBy(() -> paymentService.getById(1L, other))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(paymentService.getById(1L, admin).getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("failPayment() is idempotent and preserves the first failure reason")
  void failPayment_idempotent() {
    payment.setStatus(PaymentStatus.FAILED);
    payment.setFailureReason("Original reason");
    when(paymentRepository.findByTransactionIdWithLock("TXN-1")).thenReturn(Optional.of(payment));

    PaymentResponse response = paymentService.failPayment("TXN-1", "Later reason");

    assertThat(response.getFailureReason()).isEqualTo("Original reason");
    verify(paymentRepository, never()).save(any());
  }

  @Test
  @DisplayName("failPayment() rejects completed payments")
  void failPayment_completed_throws() {
    payment.setStatus(PaymentStatus.COMPLETED);
    when(paymentRepository.findByTransactionIdWithLock("TXN-1")).thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> paymentService.failPayment("TXN-1", "Late callback"))
        .isInstanceOf(PaymentException.class);
  }
}
