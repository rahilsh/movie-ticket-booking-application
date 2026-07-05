package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.ShowSeat;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShowSeatResponse {
  private Long id;
  private String seatLabel;
  private int rowNumber;
  private int colNumber;
  private String seatType;
  private String status;

  public static ShowSeatResponse from(ShowSeat showSeat) {
    return ShowSeatResponse.builder()
        .id(showSeat.getId())
        .seatLabel(showSeat.getSeat().getLabel())
        .rowNumber(showSeat.getSeat().getRowNumber())
        .colNumber(showSeat.getSeat().getColNumber())
        .seatType(showSeat.getSeat().getType().name())
        .status(showSeat.getStatus().name())
        .build();
  }
}
