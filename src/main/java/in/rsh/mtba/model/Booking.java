package in.rsh.mtba.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class Booking {

  private final int bookingId;
  private final int userId;
  private final int showId;
  private final int amount;
  private final List<String> seats;
  private final BookingStatus status;

  public enum BookingStatus {
    PROCESSING,
    PAYMENT_INITIATED,
    PAYMENT_FAILED,
    CANCELLED,
    COMPLETED
  }
}
