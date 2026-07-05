package com.rsh.mtba.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class BookingRequest {

  @NotNull(message = "Show ID is required")
  private Long showId;

  @NotEmpty(message = "At least one seat must be selected")
  private List<String> seatLabels; // e.g. ["A1", "A2"]
}
