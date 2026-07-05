package com.rsh.mtba.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_seats_screen_label",
          columnNames = {"screen_id", "label"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String label; // e.g. "A1", "B3"

  @Column(nullable = false)
  private int rowNumber;

  @Column(nullable = false)
  private int colNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private SeatType type = SeatType.REGULAR;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "screen_id", nullable = false)
  private Screen screen;

  public enum SeatType {
    REGULAR,
    PREMIUM,
    RECLINER,
    BLOCKED
  }
}
