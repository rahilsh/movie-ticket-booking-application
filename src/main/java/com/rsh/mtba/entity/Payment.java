package com.rsh.mtba.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String transactionId; // external payment gateway reference

  @Column(nullable = false)
  private int amountInPaise;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private PaymentStatus status = PaymentStatus.INITIATED;

  @Column private String failureReason;

  @Column(nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column private LocalDateTime completedAt;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  public enum PaymentStatus {
    INITIATED,
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
  }
}
