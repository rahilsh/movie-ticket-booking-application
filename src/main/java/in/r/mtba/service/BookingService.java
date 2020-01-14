package in.r.mtba.service;

import in.r.mtba.model.Booking;
import in.r.mtba.model.Booking.BookingStatus;
import in.r.mtba.model.ShowSeat;
import in.r.mtba.model.ShowSeat.ShowSeatStatus;
import in.r.mtba.store.GenericStore;
import in.r.mtba.store.StoreFactory;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;

public class BookingService {
  private final GenericStore<Booking> bookingStore =
      StoreFactory.getInstance().getStore(Booking.class);
  // TODO: use ShowSeatService instead of ShowSeatStore
  private final GenericStore<ShowSeat> showSeatStore =
      StoreFactory.getInstance().getStore(ShowSeat.class);

  public synchronized Booking book(int userId, int showId, List<String> seats) {
    synchronized (this) {
      List<ShowSeat> showSeats = validateAndGetAvailableSeats(showId, seats);
      markSeatsAsTU(showSeats);
    }
    Booking booking = buildBooking(userId, showId, seats);
    bookingStore.put(booking.getBookingId(), booking);
    return booking;
  }

  private Booking buildBooking(int userId, int showId, List<String> seats) {
    int bookingId = Integer.parseInt(RandomStringUtils.randomNumeric(7));
    return Booking.builder()
        .bookingId(bookingId)
        .showId(showId)
        .userId(userId)
        .amount(seats.size() * 100)
        .status(BookingStatus.PROCESSING)
        .seats(seats)
        .build();
  }

  private void markSeatsAsTU(List<ShowSeat> showSeats) {
    showSeats.forEach(
        showSeat ->
            showSeatStore.update(
                showSeat.getSeatId(),
                showSeat.toBuilder().status(ShowSeatStatus.TEMPORARILY_UNAVAILABLE).build()));
  }

  private List<ShowSeat> validateAndGetAvailableSeats(int showId, List<String> seats) {
    List<ShowSeat> showSeats =
        showSeatStore.getAll().stream()
            .filter(showSeat -> showSeat.getShowId() == showId)
            .filter(showSeat -> showSeat.getStatus().equals(ShowSeatStatus.AVAILABLE))
            .filter(showSeat -> seats.contains(showSeat.getSeatId()))
            .collect(Collectors.toList());
    if (showSeats.size() != seats.size()) {
      throw new RuntimeException("All seats not available");
    }
    return showSeats;
  }

  public Booking markBookingCompleted(Booking booking) {
    Booking updatedBooking = booking.toBuilder().status(BookingStatus.COMPLETED).build();
    bookingStore.update(booking.getBookingId(), updatedBooking);
    return updatedBooking;
  }

  public List<Booking> getAllBookingsOfUser(int userId) {
    return bookingStore.getAll().stream()
        .filter(booking -> booking.getUserId() == userId)
        .collect(Collectors.toList());
  }

  public Booking get(int bookingId) {
    return bookingStore.get(bookingId);
  }
}
