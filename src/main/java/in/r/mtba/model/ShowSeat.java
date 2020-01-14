package in.r.mtba.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder(toBuilder = true)
public class ShowSeat {
  private final String seatId;
  private final int showId;
  private final ShowSeatStatus status;

  public enum ShowSeatStatus {
    AVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    PERMANENTLY_UNAVAILABLE
  }
}
