package in.rsh.mtba.service;

import in.rsh.mtba.model.Booking;
import in.rsh.mtba.model.Booking.BookingStatus;
import in.rsh.mtba.model.ShowSeat;
import in.rsh.mtba.model.ShowSeat.ShowSeatStatus;
import in.rsh.mtba.store.GenericStore;
import in.rsh.mtba.store.StoreFactory;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class BookingService {
    private final GenericStore<Booking> bookingStore =
            StoreFactory.getInstance().getStore(Booking.class);

    private final ShowService showService = new ShowService();

    public synchronized Booking book(int userId, int showId, List<String> seats) {
        synchronized (this) {
            List<ShowSeat> showSeats = validateAndGetAvailableSeats(showId, seats);
            markSeatsAsTU(showSeats);
        }
        Booking booking = buildBooking(userId, showId, seats);
        bookingStore.put(booking.getBookingId(), booking);
        return booking;
    }

    public void cancelBookings() {
        bookingStore.deleteAll();
    }

    public void cancelBooking(Booking booking) {
        booking.setStatus(BookingStatus.CANCELLED);
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
                        showSeat.setStatus(ShowSeatStatus.TEMPORARILY_UNAVAILABLE));
    }

    private List<ShowSeat> validateAndGetAvailableSeats(int showId, List<String> seats) {
        List<ShowSeat> showSeats =
                showService.getSeatsForShows(showId).stream()
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
