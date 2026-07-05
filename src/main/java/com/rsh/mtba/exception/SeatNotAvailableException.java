package com.rsh.mtba.exception;

import java.util.List;

public class SeatNotAvailableException extends RuntimeException {

  public SeatNotAvailableException(String message) {
    super(message);
  }

  public SeatNotAvailableException(List<String> seatLabels) {
    super("The following seats are not available: " + String.join(", ", seatLabels));
  }
}
