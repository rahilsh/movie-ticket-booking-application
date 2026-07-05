package com.rsh.mtba.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

  private Long id;
  private String transactionId;
  private Long bookingId;
  private int amountInPaise;

  @Builder.Default
  private PaymentStatus status = PaymentStatus.INITIATED;

  private String failureReason;

  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime completedAt;

  public enum PaymentStatus {
    INITIATED,
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
  }
}
