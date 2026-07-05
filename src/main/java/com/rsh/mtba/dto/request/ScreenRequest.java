package com.rsh.mtba.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScreenRequest {

  @NotBlank(message = "Screen name is required")
  private String name;

  @Min(value = 1, message = "Rows must be at least 1")
  private int rows;

  @Min(value = 1, message = "Columns must be at least 1")
  private int cols;
}
