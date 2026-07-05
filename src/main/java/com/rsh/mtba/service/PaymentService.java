package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.PaymentRequest;
import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.PaymentException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final BookingRepository bookingRepository;
  private final BookingService bookingService;
  private final ShowSeatRepository showSeatRepository;

  @Transactional
  public PaymentResponse initiatePayment(Long userId, PaymentRequest request) {
    Booking booking = bookingService.findById(request.getBookingId());

    if (!booking.getUserId().equals(userId)) {
      throw new BookingException("You can only pay for your own bookings");
    }
    if (booking.getStatus() != BookingStatus.PROCESSING) {
      throw new BookingException("Booking is not in PROCESSING state: " + booking.getStatus());
    }

    String transactionId = (request.getTransactionId() != null && !request.getTransactionId().isBlank())
        ? request.getTransactionId()
        : "TXN-" + RandomStringUtils.randomAlphanumeric(12).toUpperCase();

    Payment payment = Payment.builder()
        .transactionId(transactionId)
        .bookingId(booking.getId())
        .amountInPaise(booking.getTotalAmountInPaise())
        .status(PaymentStatus.INITIATED)
        .build();
    Payment saved = paymentRepository.save(payment);

    booking.setStatus(BookingStatus.PAYMENT_INITIATED);
    bookingRepository.save(booking);

    log.info("Initiated payment id={} transactionId={} bookingId={}",
        saved.getId(), transactionId, booking.getId());
    return PaymentResponse.from(saved);
  }

  @Transactional
  public PaymentResponse confirmPayment(String transactionId) {
    Payment payment = paymentRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Payment not found for transactionId: " + transactionId));

    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      return PaymentResponse.from(payment);
    }
    if (payment.getStatus() == PaymentStatus.FAILED) {
      throw new PaymentException("Payment already failed for transactionId: " + transactionId);
    }

    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setCompletedAt(LocalDateTime.now());
    Payment updated = paymentRepository.save(payment);

    bookingService.confirmBooking(payment.getBookingId());

    log.info("Confirmed payment id={} transactionId={}", updated.getId(), transactionId);
    return PaymentResponse.from(updated);
  }

  @Transactional
  public PaymentResponse failPayment(String transactionId, String reason) {
    Payment payment = paymentRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Payment not found for transactionId: " + transactionId));

    payment.setStatus(PaymentStatus.FAILED);
    payment.setFailureReason(reason);
    Payment updated = paymentRepository.save(payment);

    Booking booking = bookingService.findById(payment.getBookingId());
    booking.setStatus(BookingStatus.PAYMENT_FAILED);
    bookingRepository.save(booking);

    // Release seats back to AVAILABLE
    List<ShowSeat> seats = showSeatRepository.findByShowId(booking.getShowId()).stream()
        .filter(ss -> booking.getSeatLabels().contains(ss.getSeatLabel()))
        .collect(Collectors.toList());
    seats.forEach(ss -> ss.setStatus(ShowSeat.ShowSeatStatus.AVAILABLE));
    showSeatRepository.saveAll(seats);

    log.warn("Payment failed id={} transactionId={} reason={}", updated.getId(), transactionId, reason);
    return PaymentResponse.from(updated);
  }

  public PaymentResponse getByBookingId(Long bookingId) {
    return PaymentResponse.from(
        paymentRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Payment not found for bookingId: " + bookingId)));
  }

  public PaymentResponse getById(Long id) {
    return PaymentResponse.from(
        paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", id)));
  }
}
