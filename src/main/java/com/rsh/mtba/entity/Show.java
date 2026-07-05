package com.rsh.mtba.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(
    name = "shows",
    indexes = {
      @Index(name = "idx_shows_screen_id", columnList = "screen_id"),
      @Index(name = "idx_shows_start_time", columnList = "start_time")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String movieName;

  @NotNull
  @Column(nullable = false)
  private LocalDateTime startTime;

  @NotNull
  @Column(nullable = false)
  private LocalDateTime endTime;

  @Column(nullable = false)
  private int basePriceInPaise; // price per seat in paise (e.g. 25000 = ₹250)

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "screen_id", nullable = false)
  private Screen screen;

  @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ShowSeat> showSeats;
}
