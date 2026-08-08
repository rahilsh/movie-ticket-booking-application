package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.PaymentRequest;
import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.entity.User.Role;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.PaymentException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
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
    Booking booking = bookingService.findByIdWithLock(request.getBookingId());

    if (!booking.getUserId().equals(userId)) {
      throw new BookingException("You can only pay for your own bookings");
    }
    if (booking.getStatus() == BookingStatus.PAYMENT_INITIATED) {
      Payment existing = paymentRepository.findByBookingId(booking.getId())
          .orElseThrow(() -> new PaymentException("Payment state is inconsistent"));
      if (request.getTransactionId() == null || request.getTransactionId().isBlank()
          || existing.getTransactionId().equals(request.getTransactionId())) {
        return PaymentResponse.from(existing);
      }
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
    Payment existing = paymentRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Payment not found for transactionId: " + transactionId));
    bookingService.findByIdWithLock(existing.getBookingId());
    Payment payment = paymentRepository.findByTransactionIdWithLock(transactionId)
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
    Payment existing = paymentRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Payment not found for transactionId: " + transactionId));
    Booking booking = bookingService.findByIdWithLock(existing.getBookingId());
    Payment payment = paymentRepository.findByTransactionIdWithLock(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Payment not found for transactionId: " + transactionId));

    if (payment.getStatus() == PaymentStatus.FAILED) {
      return PaymentResponse.from(payment);
    }
    if (payment.getStatus() == PaymentStatus.COMPLETED
        || payment.getStatus() == PaymentStatus.REFUNDED) {
      throw new PaymentException("Completed payment cannot be failed: " + transactionId);
    }

    payment.setStatus(PaymentStatus.FAILED);
    payment.setFailureReason(reason);
    Payment updated = paymentRepository.save(payment);

    booking.setStatus(BookingStatus.PAYMENT_FAILED);
    bookingRepository.save(booking);

    showSeatRepository.releaseOwnedSeats(booking.getId());

    log.warn("Payment failed id={} transactionId={} reason={}", updated.getId(), transactionId, reason);
    return PaymentResponse.from(updated);
  }

  public PaymentResponse getByBookingId(Long bookingId, User requestingUser) {
    authorize(bookingService.findById(bookingId), requestingUser);
    return PaymentResponse.from(
        paymentRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Payment not found for bookingId: " + bookingId)));
  }

  public PaymentResponse getById(Long id, User requestingUser) {
    Payment payment = paymentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    authorize(bookingService.findById(payment.getBookingId()), requestingUser);
    return PaymentResponse.from(payment);
  }

  private void authorize(Booking booking, User requestingUser) {
    if (!booking.getUserId().equals(requestingUser.getId())
        && requestingUser.getRole() != Role.ROLE_ADMIN) {
      throw new AccessDeniedException("Payment belongs to another user");
    }
  }
}
