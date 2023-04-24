package in.rsh.mtba;

import in.rsh.mtba.model.*;
import in.rsh.mtba.service.BookingService;
import in.rsh.mtba.service.OnBoardService;
import in.rsh.mtba.service.PaymentService;
import in.rsh.mtba.service.ShowService;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// TODO: Add more test cases
public class AppTest {

  private final OnBoardService onBoardService = new OnBoardService();

  private final ShowService showService = new ShowService();
  private final BookingService bookingService = new BookingService();
  private final PaymentService paymentService = new PaymentService();
  private Show dummyShow;
  String[][] dummySeatLayout = {{"a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10"}};

  @Before
  public void setUp() {
    Theatre dummyTheatre = addDummyTheatre();
    Screen dummyScreen = addDummyScreen(dummyTheatre, dummySeatLayout);
    dummyShow = addDummyShow(dummyScreen);
  }

  @After
  public void tearDown() {
    cancelAllBookings();
  }

  private Theatre addDummyTheatre() {
    return onBoardService.addTheatre(Theatre.builder().theatreId(1).name("test").address("Bangalore").build());
  }

  private Show addDummyShow(Screen dummyScreen) {
    return onBoardService.addShow(
        Show.builder()
                .showId(1)
            .screenId(dummyScreen.getScreenId())
            .startTime(LocalDateTime.of(2020, Month.JANUARY, 30, 0, 0))
            .endTime(LocalDateTime.of(2020, Month.JANUARY, 30, 2, 0))
            .build());
  }

  private Screen addDummyScreen(Theatre dummyTheatre, String[][] seatLayout) {
    return onBoardService.addScreen(
        Screen.builder()
                .screenId(1)
            .name("S1")
            .theatreId(dummyTheatre.getTheatreId())
            .totalCapacity(10)
            .breadth(500)
            .length(50)
            .seatLayout(seatLayout)
            .build());
  }

  @Test
  public void bookTickets() {
    List<Integer> users = getDummyUsers();
    users
        .parallelStream()
        .forEach(
            user -> {
              System.out.println("Processing user: " + user);
              printSeats();
              List<String> seatsToBook = getUniqueSeatsForEachUser(user);
              bookAndPay(user, seatsToBook);
              printSeats();
            });
  }

  private List<Integer> getDummyUsers() {
    List<Integer> users = new ArrayList<>();
    users.add(1);
    users.add(2);
    return users;
  }

  private List<String> getUniqueSeatsForEachUser(Integer user) {
    List<String> seatsToBook = new ArrayList<>();
    seatsToBook.add("a" + user);
    return seatsToBook;
  }

  @Test(expected = RuntimeException.class)
  public void bookTickets_throwsException() {
    List<Integer> users = getDummyUsers();
    users
        .parallelStream()
        .forEach(
            userId -> {
              System.out.println("Processing user: " + userId);
              printSeats();
              List<String> seatsToBook = getSameSeatForAllUser();
              bookAndPay(userId, seatsToBook);
              printSeats();
            });
  }

  private void bookAndPay(Integer userId, List<String> seatsToBook) {
    Booking booking = bookingService.book(userId, dummyShow.getShowId(), seatsToBook);
    Payment payment = paymentService.pay(booking);
    List<ShowSeat> seatsForShows = showService.getSeatsForShows(booking.getShowId());
    seatsForShows.forEach(showSeat -> {
      if (seatsToBook.contains(showSeat.getSeatId())){
        showSeat.setStatus(ShowSeat.ShowSeatStatus.PERMANENTLY_UNAVAILABLE);
      }
    });
    bookingService.markBookingCompleted(booking);
    Booking updatedBooking = bookingService.get(booking.getBookingId());
    System.out.println(updatedBooking);
    System.out.println(payment);
  }

  private void cancelAllBookings() {
    bookingService.cancelBookings();
    showService.getSeats().forEach(
            showSeat ->
                    showSeat.setStatus(ShowSeat.ShowSeatStatus.AVAILABLE));
    paymentService.deletePayments();

  }
  private List<String> getSameSeatForAllUser() {
    List<String> seatsToBook = new ArrayList<>();
    seatsToBook.add("a1");
    return seatsToBook;
  }

  private void printSeats() {
    showService
        .getSeatsForShows(dummyShow.getShowId())
        .forEach(showSeat -> System.out.println(showSeat.getSeatId() + "-" + showSeat.getStatus()));
  }
}
