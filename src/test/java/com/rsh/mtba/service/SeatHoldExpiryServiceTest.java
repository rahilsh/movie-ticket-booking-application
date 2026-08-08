package com.rsh.mtba.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeatHoldExpiryServiceTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ShowSeatRepository showSeatRepository;
  @InjectMocks private SeatHoldExpiryService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "batchSize", 10);
  }

  @Test
  void expireDueBookings_releasesSeatsAndFailsPayment() {
    Booking booking = Booking.builder().id(10L).status(BookingStatus.PAYMENT_INITIATED).build();
    Payment payment = Payment.builder().id(20L).status(PaymentStatus.INITIATED).build();
    when(bookingRepository.findExpiredWithLock(10)).thenReturn(List.of(booking));
    when(paymentRepository.findByBookingIdWithLock(10L)).thenReturn(Optional.of(payment));
    when(bookingRepository.save(any())).thenReturn(booking);
    when(paymentRepository.save(any())).thenReturn(payment);

    service.expireDueBookings();

    assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(payment.getFailureReason()).isEqualTo("Seat hold expired");
    verify(showSeatRepository).releaseOwnedSeats(10L);
  }

  @Test
  void expireDueBookings_doesNotRewriteTerminalPayment() {
    Booking booking = Booking.builder().id(10L).status(BookingStatus.PROCESSING).build();
    Payment payment = Payment.builder().id(20L).status(PaymentStatus.COMPLETED).build();
    when(bookingRepository.findExpiredWithLock(10)).thenReturn(List.of(booking));
    when(paymentRepository.findByBookingIdWithLock(10L)).thenReturn(Optional.of(payment));
    when(bookingRepository.save(any())).thenReturn(booking);

    service.expireDueBookings();

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    verify(showSeatRepository).releaseOwnedSeats(10L);
  }

  @Test
  void expireDueBookings_failsPendingPayment() {
    Booking booking = Booking.builder().id(10L).status(BookingStatus.PROCESSING).build();
    Payment payment = Payment.builder().id(20L).status(PaymentStatus.PENDING).build();
    when(bookingRepository.findExpiredWithLock(10)).thenReturn(List.of(booking));
    when(paymentRepository.findByBookingIdWithLock(10L)).thenReturn(Optional.of(payment));
    when(bookingRepository.save(any())).thenReturn(booking);
    when(paymentRepository.save(any())).thenReturn(payment);

    service.expireDueBookings();

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
  }
}
