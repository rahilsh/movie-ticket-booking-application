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
public class ShowSeat {

  private Long id;
  private Long showId;
  private Long seatId;
  private String seatLabel;
  private int rowNumber;
  private int colNumber;
  private Seat.SeatType seatType;

  @Builder.Default
  private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;

  private Long version;

  public enum ShowSeatStatus {
    AVAILABLE,
    LOCKED,
    BOOKED
  }
}
