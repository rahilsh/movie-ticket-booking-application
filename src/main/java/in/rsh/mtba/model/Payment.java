package in.rsh.mtba.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Payment {

  private final String paymentId;
  private final int bookingId;
  private final int amountInPaise;
  private final PaymentStatus status;

  public enum PaymentStatus {
    INITIATED,
    PENDING,
    FAILED,
    COMPLETED
  }
}
