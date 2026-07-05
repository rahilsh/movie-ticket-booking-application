package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.Screen;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScreenResponse {
  private Long id;
  private String name;
  private int totalCapacity;
  private int rows;
  private int cols;
  private Long theatreId;
  private String theatreName;

  public static ScreenResponse from(Screen screen) {
    return ScreenResponse.builder()
        .id(screen.getId())
        .name(screen.getName())
        .totalCapacity(screen.getTotalCapacity())
        .rows(screen.getRows())
        .cols(screen.getCols())
        .theatreId(screen.getTheatreId())
        .theatreName(screen.getTheatreName())
        .build();
  }
}
