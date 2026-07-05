package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.Payment;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
  private Long id;
  private String transactionId;
  private Long bookingId;
  private int amountInPaise;
  private String status;
  private String failureReason;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;

  public static PaymentResponse from(Payment payment) {
    return PaymentResponse.builder()
        .id(payment.getId())
        .transactionId(payment.getTransactionId())
        .bookingId(payment.getBooking().getId())
        .amountInPaise(payment.getAmountInPaise())
        .status(payment.getStatus().name())
        .failureReason(payment.getFailureReason())
        .createdAt(payment.getCreatedAt())
        .completedAt(payment.getCompletedAt())
        .build();
  }
}
