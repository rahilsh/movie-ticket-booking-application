package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.BookingRequest;
import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Show;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.entity.User.Role;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.exception.SeatNotAvailableException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.PaymentRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final ShowSeatRepository showSeatRepository;
  private final PaymentRepository paymentRepository;
  private final ShowService showService;
  private final UserService userService;

  @Value("${app.booking.seat-hold-duration:PT10M}")
  private Duration seatHoldDuration = Duration.ofMinutes(10);

  /**
   * Creates a booking and locks the requested seats with SELECT ... FOR UPDATE.
   * Prevents double-booking under concurrent requests across multiple nodes.
   */
  @Transactional
  public BookingResponse book(Long userId, BookingRequest request) {
    userService.findById(userId); // validate user exists
    Show show = showService.findById(request.getShowId());

    // Acquire pessimistic write lock at DB level
    List<ShowSeat> showSeats = showSeatRepository
        .findByShowIdAndSeatLabelInWithLock(request.getShowId(), request.getSeatLabels());

    validateSeats(request.getSeatLabels(), showSeats);

    // Mark seats LOCKED
    showSeats.forEach(ss -> ss.setStatus(ShowSeatStatus.LOCKED));
    showSeatRepository.saveAll(showSeats);

    int totalAmount = showSeats.size() * show.getBasePriceInPaise();

    Booking booking = Booking.builder()
        .userId(userId)
        .showId(show.getId())
        .movieName(show.getMovieName())
        .totalAmountInPaise(totalAmount)
        .status(BookingStatus.PROCESSING)
        .holdExpiresAt(LocalDateTime.now().plus(seatHoldDuration))
        .build();
    Booking saved = bookingRepository.save(booking);

    showSeats.forEach(ss -> ss.setOwnerBookingId(saved.getId()));
    showSeatRepository.saveAll(showSeats);

    // Persist the booking ↔ show_seat join records
    List<Long> showSeatIds = showSeats.stream().map(ShowSeat::getId).collect(Collectors.toList());
    bookingRepository.saveBookingSeats(saved.getId(), showSeatIds);
    saved.setSeatLabels(request.getSeatLabels());

    log.info("Created booking id={} userId={} showId={} seats={}",
        saved.getId(), userId, request.getShowId(), request.getSeatLabels());
    return BookingResponse.from(saved);
  }

  @Transactional
  public BookingResponse confirmBooking(Long bookingId) {
    Booking booking = findByIdWithLock(bookingId);
    if (booking.getStatus() != BookingStatus.PAYMENT_INITIATED) {
      throw new BookingException("Booking is not in PAYMENT_INITIATED state: " + booking.getStatus());
    }
    booking.setStatus(BookingStatus.COMPLETED);
    bookingRepository.save(booking);

    int expected = showSeatRepository.countByBookingId(bookingId);
    int updated = showSeatRepository.markOwnedSeatsBooked(bookingId);
    if (updated != expected) {
      throw new BookingException("Booking no longer owns all requested seats");
    }

    log.info("Confirmed booking id={}", bookingId);
    return BookingResponse.from(booking);
  }

  @Transactional
  public BookingResponse cancelBooking(Long bookingId, Long requestingUserId) {
    Booking booking = findByIdWithLock(bookingId);

    if (!booking.getUserId().equals(requestingUserId)) {
      throw new BookingException("You can only cancel your own bookings");
    }
    if (booking.getStatus() == BookingStatus.COMPLETED) {
      throw new BookingException("Cannot cancel a completed booking");
    }
    if (booking.getStatus() == BookingStatus.CANCELLED) {
      throw new BookingException("Booking is already cancelled");
    }

    if (booking.getStatus() == BookingStatus.PAYMENT_INITIATED) {
      paymentRepository.findByBookingIdWithLock(bookingId).ifPresent(payment -> {
        if (payment.getStatus() == com.rsh.mtba.entity.Payment.PaymentStatus.INITIATED
            || payment.getStatus() == com.rsh.mtba.entity.Payment.PaymentStatus.PENDING) {
          payment.setStatus(com.rsh.mtba.entity.Payment.PaymentStatus.FAILED);
          payment.setFailureReason("Booking cancelled");
          paymentRepository.save(payment);
        }
      });
    }

    booking.setStatus(BookingStatus.CANCELLED);
    bookingRepository.save(booking);

    showSeatRepository.releaseOwnedSeats(bookingId);

    log.info("Cancelled booking id={}", bookingId);
    return BookingResponse.from(booking);
  }

  public BookingResponse getById(Long id, User requestingUser) {
    Booking booking = findById(id);
    if (!booking.getUserId().equals(requestingUser.getId())
        && requestingUser.getRole() != Role.ROLE_ADMIN) {
      throw new AccessDeniedException("Booking belongs to another user");
    }
    return BookingResponse.from(booking);
  }

  public List<BookingResponse> getByUser(Long userId) {
    userService.findById(userId);
    return bookingRepository.findByUserId(userId).stream()
        .map(BookingResponse::from)
        .collect(Collectors.toList());
  }

  public Booking findById(Long id) {
    return bookingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
  }

  public Booking findByIdWithLock(Long id) {
    return bookingRepository.findByIdWithLock(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
  }

  private void validateSeats(List<String> requestedLabels, List<ShowSeat> fetchedSeats) {
    if (fetchedSeats.size() != requestedLabels.size()) {
      List<String> foundLabels = fetchedSeats.stream()
          .map(ShowSeat::getSeatLabel).collect(Collectors.toList());
      List<String> missing = requestedLabels.stream()
          .filter(l -> !foundLabels.contains(l)).collect(Collectors.toList());
      throw new ResourceNotFoundException("Seats not found for this show: " + missing);
    }
    List<String> unavailable = fetchedSeats.stream()
        .filter(ss -> ss.getStatus() != ShowSeatStatus.AVAILABLE)
        .map(ShowSeat::getSeatLabel)
        .collect(Collectors.toList());
    if (!unavailable.isEmpty()) {
      throw new SeatNotAvailableException(unavailable);
    }
  }
}
