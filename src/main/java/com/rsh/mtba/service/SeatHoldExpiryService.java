package com.rsh.mtba.service;

import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.booking.expiry-enabled", havingValue = "true", matchIfMissing = true)
public class SeatHoldExpiryService {

  private final BookingRepository bookingRepository;
  private final PaymentRepository paymentRepository;
  private final ShowSeatRepository showSeatRepository;

  @Value("${app.booking.expiry-batch-size:100}")
  private int batchSize;

  @Scheduled(fixedDelayString = "${app.booking.expiry-scan-delay-ms:30000}")
  @Transactional
  public void expireDueBookings() {
    for (Booking booking : bookingRepository.findExpiredWithLock(batchSize)) {
      paymentRepository.findByBookingIdWithLock(booking.getId()).ifPresent(this::failPayment);
      booking.setStatus(BookingStatus.EXPIRED);
      bookingRepository.save(booking);
      showSeatRepository.releaseOwnedSeats(booking.getId());
      log.info("Expired booking id={} and released its seat hold", booking.getId());
    }
  }

  private void failPayment(Payment payment) {
    if (payment.getStatus() == PaymentStatus.INITIATED
        || payment.getStatus() == PaymentStatus.PENDING) {
      payment.setStatus(PaymentStatus.FAILED);
      payment.setFailureReason("Seat hold expired");
      paymentRepository.save(payment);
    }
  }
}
