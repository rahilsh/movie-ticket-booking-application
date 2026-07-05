package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.BookingRequest;
import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import com.rsh.mtba.entity.Show;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.exception.SeatNotAvailableException;
import com.rsh.mtba.repository.BookingRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

  private final BookingRepository bookingRepository;
  private final ShowSeatRepository showSeatRepository;
  private final ShowService showService;
  private final UserService userService;

  /**
   * Creates a booking and locks the requested seats using pessimistic DB-level locking. This
   * prevents double-booking under concurrent requests without relying on JVM-level synchronization
   * (which breaks in a multi-node deployment).
   */
  @Transactional
  public BookingResponse book(Long userId, BookingRequest request) {
    User user = userService.findById(userId);
    Show show = showService.findById(request.getShowId());

    // Acquire pessimistic write lock on the target seats to prevent concurrent booking
    List<ShowSeat> showSeats =
        showSeatRepository.findByShowIdAndSeatLabelInWithLock(
            request.getShowId(), request.getSeatLabels());

    validateSeats(request.getSeatLabels(), showSeats);

    // Mark seats as LOCKED while booking is in progress
    showSeats.forEach(seat -> seat.setStatus(ShowSeatStatus.LOCKED));
    showSeatRepository.saveAll(showSeats);

    int totalAmount = showSeats.size() * show.getBasePriceInPaise();

    Booking booking =
        Booking.builder()
            .user(user)
            .show(show)
            .showSeats(showSeats)
            .totalAmountInPaise(totalAmount)
            .status(BookingStatus.PROCESSING)
            .build();

    Booking saved = bookingRepository.save(booking);
    log.info(
        "Created booking id={} userId={} showId={} seats={}",
        saved.getId(),
        userId,
        request.getShowId(),
        request.getSeatLabels());
    return BookingResponse.from(saved);
  }

  @Transactional
  public BookingResponse confirmBooking(Long bookingId) {
    Booking booking = findById(bookingId);
    if (booking.getStatus() != BookingStatus.PAYMENT_INITIATED) {
      throw new BookingException(
          "Booking is not in PAYMENT_INITIATED state: " + booking.getStatus());
    }
    booking.setStatus(BookingStatus.COMPLETED);
    booking.getShowSeats().forEach(seat -> seat.setStatus(ShowSeatStatus.BOOKED));
    showSeatRepository.saveAll(booking.getShowSeats());
    Booking updated = bookingRepository.save(booking);
    log.info("Confirmed booking id={}", bookingId);
    return BookingResponse.from(updated);
  }

  @Transactional
  public BookingResponse cancelBooking(Long bookingId, Long requestingUserId) {
    Booking booking = findById(bookingId);

    if (!booking.getUser().getId().equals(requestingUserId)) {
      throw new BookingException("You can only cancel your own bookings");
    }
    if (booking.getStatus() == BookingStatus.COMPLETED) {
      throw new BookingException("Cannot cancel a completed booking");
    }
    if (booking.getStatus() == BookingStatus.CANCELLED) {
      throw new BookingException("Booking is already cancelled");
    }

    booking.setStatus(BookingStatus.CANCELLED);
    // Release the seats back to AVAILABLE
    booking.getShowSeats().forEach(seat -> seat.setStatus(ShowSeatStatus.AVAILABLE));
    showSeatRepository.saveAll(booking.getShowSeats());
    Booking updated = bookingRepository.save(booking);
    log.info("Cancelled booking id={}", bookingId);
    return BookingResponse.from(updated);
  }

  @Transactional(readOnly = true)
  public BookingResponse getById(Long id) {
    return BookingResponse.from(findById(id));
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> getByUser(Long userId) {
    userService.findById(userId);
    return bookingRepository.findByUserId(userId).stream()
        .map(BookingResponse::from)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Booking findById(Long id) {
    return bookingRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
  }

  private void validateSeats(List<String> requestedLabels, List<ShowSeat> fetchedSeats) {
    if (fetchedSeats.size() != requestedLabels.size()) {
      List<String> foundLabels =
          fetchedSeats.stream().map(ss -> ss.getSeat().getLabel()).collect(Collectors.toList());
      List<String> missing =
          requestedLabels.stream()
              .filter(l -> !foundLabels.contains(l))
              .collect(Collectors.toList());
      throw new ResourceNotFoundException("Seats not found for this show: " + missing);
    }

    List<String> unavailable =
        fetchedSeats.stream()
            .filter(ss -> ss.getStatus() != ShowSeatStatus.AVAILABLE)
            .map(ss -> ss.getSeat().getLabel())
            .collect(Collectors.toList());

    if (!unavailable.isEmpty()) {
      throw new SeatNotAvailableException(unavailable);
    }
  }
}
