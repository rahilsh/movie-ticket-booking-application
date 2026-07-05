package com.rsh.mtba.entity;

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
public class Seat {

  private Long id;
  private String label;
  private int rowNumber;
  private int colNumber;

  @Builder.Default
  private SeatType type = SeatType.REGULAR;

  private Long screenId;

  public enum SeatType {
    REGULAR, PREMIUM, RECLINER, BLOCKED
  }
}
