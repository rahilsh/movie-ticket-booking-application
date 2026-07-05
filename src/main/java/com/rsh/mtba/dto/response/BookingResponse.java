package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.Booking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResponse {
  private Long id;
  private Long userId;
  private Long showId;
  private String movieName;
  private int totalAmountInPaise;
  private String status;
  private LocalDateTime createdAt;
  private List<String> seatLabels;

  public static BookingResponse from(Booking booking) {
    return BookingResponse.builder()
        .id(booking.getId())
        .userId(booking.getUser().getId())
        .showId(booking.getShow().getId())
        .movieName(booking.getShow().getMovieName())
        .totalAmountInPaise(booking.getTotalAmountInPaise())
        .status(booking.getStatus().name())
        .createdAt(booking.getCreatedAt())
        .seatLabels(
            booking.getShowSeats().stream()
                .map(ss -> ss.getSeat().getLabel())
                .collect(Collectors.toList()))
        .build();
  }
}
