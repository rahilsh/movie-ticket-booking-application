package com.rsh.mtba.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

  @NotNull(message = "Booking ID is required")
  private Long bookingId;

  /** Simulated external transaction ID returned from a payment gateway */
  private String transactionId;
}
