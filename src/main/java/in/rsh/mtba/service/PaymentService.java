package in.rsh.mtba.service;

import in.rsh.mtba.model.Booking;
import in.rsh.mtba.model.Payment;
import in.rsh.mtba.model.Payment.PaymentStatus;
import in.rsh.mtba.model.ShowSeat;
import in.rsh.mtba.model.ShowSeat.ShowSeatStatus;
import in.rsh.mtba.store.GenericStore;
import in.rsh.mtba.store.StoreFactory;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;

public class PaymentService {

  private final GenericStore<Payment> paymentStore =
      StoreFactory.getInstance().getStore(Payment.class);
  private final BookingService bookingService = new BookingService();
  // TODO: use ShowSeatService instead of ShowSeatStore
  private final GenericStore<ShowSeat> showSeatStore =
      StoreFactory.getInstance().getStore(ShowSeat.class);

  public Payment pay(Booking booking) {
    Payment payment = makePayment(booking);
    bookingService.markBookingCompleted(booking);
    markSeatsBooked(booking);
    return payment;
  }

  private Payment makePayment(Booking booking) {
    String paymentId = RandomStringUtils.randomAlphabetic(7);
    Payment payment =
        Payment.builder()
            .paymentId(paymentId)
            .status(PaymentStatus.COMPLETED)
            .bookingId(booking.getBookingId())
            .amountInPaise(booking.getAmount())
            .build();
    paymentStore.put(paymentId, payment);
    return payment;
  }

  private void markSeatsBooked(Booking booking) {
    booking
        .getSeats()
        .forEach(
            seat ->
                showSeatStore.update(
                    seat,
                    showSeatStore
                        .get(seat)
                        .toBuilder()
                        .status(ShowSeatStatus.PERMANENTLY_UNAVAILABLE)
                        .build()));
  }

  public List<Payment> getAllPaymentsForABooking(int bookingId) {
    return paymentStore.getAll().stream()
        .filter(payment -> payment.getBookingId() == bookingId)
        .collect(Collectors.toList());
  }
}
