package com.rsh.mtba.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(
    name = "bookings",
    indexes = {
      @Index(name = "idx_bookings_user_id", columnList = "user_id"),
      @Index(name = "idx_bookings_show_id", columnList = "show_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private int totalAmountInPaise;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private BookingStatus status = BookingStatus.PROCESSING;

  @Column(nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "show_id", nullable = false)
  private Show show;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "booking_show_seats",
      joinColumns = @JoinColumn(name = "booking_id"),
      inverseJoinColumns = @JoinColumn(name = "show_seat_id"))
  private List<ShowSeat> showSeats;

  @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Payment payment;

  @PreUpdate
  void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public enum BookingStatus {
    PROCESSING,
    PAYMENT_INITIATED,
    PAYMENT_FAILED,
    CANCELLED,
    COMPLETED
  }
}
