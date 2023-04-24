package in.rsh.mtba.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder(toBuilder = true)
public class ShowSeat {
  private final String seatId;
  private final int showId;
  private ShowSeatStatus status;

  public void setStatus(ShowSeatStatus showSeatStatus) {
    this.status = showSeatStatus;
  }

  public enum ShowSeatStatus {
    AVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    PERMANENTLY_UNAVAILABLE
  }
}
