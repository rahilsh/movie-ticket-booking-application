package com.rsh.mtba.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ShowRequest {

  @NotBlank(message = "Movie name is required")
  private String movieName;

  @NotNull(message = "Start time is required")
  private LocalDateTime startTime;

  @NotNull(message = "End time is required")
  private LocalDateTime endTime;

  @Min(value = 1, message = "Base price must be at least 1 paise")
  private int basePriceInPaise;
}
