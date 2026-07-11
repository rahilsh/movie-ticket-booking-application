package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.ShowSeat;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
        .seatLabel(showSeat.getSeatLabel())
        .rowNumber(showSeat.getRowNumber())
        .colNumber(showSeat.getColNumber())
        .seatType(showSeat.getSeatType() != null ? showSeat.getSeatType().name() : null)
        .status(showSeat.getStatus().name())
        .build();
  }
}
