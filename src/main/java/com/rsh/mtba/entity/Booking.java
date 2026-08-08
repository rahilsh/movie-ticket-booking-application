package com.rsh.mtba.entity;

import java.time.LocalDateTime;
import java.util.List;
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
public class Booking {

  private Long id;
  private Long userId;
  private Long showId;
  private int totalAmountInPaise;

  @Builder.Default
  private BookingStatus status = BookingStatus.PROCESSING;

  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime updatedAt;
  private LocalDateTime holdExpiresAt;

  // Populated by join query for response building
  private String movieName;
  private List<String> seatLabels;

  public enum BookingStatus {
    PROCESSING,
    PAYMENT_INITIATED,
    PAYMENT_FAILED,
    EXPIRED,
    CANCELLED,
    COMPLETED
  }
}
