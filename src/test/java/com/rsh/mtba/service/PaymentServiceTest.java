package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.PaymentRequest;
import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.PaymentException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private BookingService bookingService;
  @Mock private ShowSeatRepository showSeatRepository;

  @InjectMocks private PaymentService paymentService;

  private Booking booking;
  private Payment payment;

  @BeforeEach
  void setUp() {
    booking = Booking.builder()
        .id(10L).userId(1L).showId(1L).movieName("Movie")
        .seatLabels(List.of("A1")).totalAmountInPaise(25000)
        .status(BookingStatus.PROCESSING).createdAt(LocalDateTime.now()).build();
    payment = Payment.builder()
        .id(1L).transactionId("TXN-1").bookingId(10L)
        .amountInPaise(25000).status(PaymentStatus.INITIATED)
        .createdAt(LocalDateTime.now()).build();
  }

  @Test
  @DisplayName("initiatePayment() creates payment and transitions booking to PAYMENT_INITIATED")
  void initiatePayment_success() {
    PaymentRequest request = new PaymentRequest();
    request.setBookingId(10L);
    request.setTransactionId("TXN-CUSTOM-123");

    when(bookingService.findByIdWithLock(10L)).thenReturn(booking);
    Payment saved = Payment.builder().id(1L).transactionId("TXN-CUSTOM-123").bookingId(10L)
        .amountInPaise(25000).status(PaymentStatus.INITIATED).createdAt(LocalDateTime.now()).build();
    when(paymentRepository.save(any())).thenReturn(saved);
    when(bookingRepository.save(any())).thenReturn(booking);

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
    when(bookingService.findByIdWithLock(10L)).thenReturn(booking);

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
    when(bookingService.findByIdWithLock(10L)).thenReturn(booking);

    assertThatThrownBy(() -> paymentService.initiatePayment(1L, request))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("PROCESSING");
  }

  @Test
  @DisplayName("confirmPayment() marks payment COMPLETED and confirms booking")
  void confirmPayment_success() {
    booking.setStatus(BookingStatus.PAYMENT_INITIATED);
    when(paymentRepository.findByTransactionIdWithLock("TXN-1")).thenReturn(Optional.of(payment));
    when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PaymentResponse response = paymentService.confirmPayment("TXN-1");

    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    verify(bookingService).confirmBooking(10L);
  }

  @Test
  @DisplayName("confirmPayment() throws PaymentException if payment already failed")
  void confirmPayment_alreadyFailed_throws() {
    payment.setStatus(PaymentStatus.FAILED);
    when(paymentRepository.findByTransactionIdWithLock("TXN-1")).thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> paymentService.confirmPayment("TXN-1"))
        .isInstanceOf(PaymentException.class)
        .hasMessageContaining("failed");
  }
}
