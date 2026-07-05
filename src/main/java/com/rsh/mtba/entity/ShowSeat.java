package com.rsh.mtba.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "show_seats",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_show_seats_show_seat",
          columnNames = {"show_id", "seat_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "show_id", nullable = false)
  private Show show;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "seat_id", nullable = false)
  private Seat seat;

  @Version private Long version; // optimistic locking

  public enum ShowSeatStatus {
    AVAILABLE,
    LOCKED, // temporarily held during booking flow
    BOOKED // permanently unavailable after successful payment
  }
}
