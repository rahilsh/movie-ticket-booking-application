package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.Show;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShowResponse {
  private Long id;
  private String movieName;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private int basePriceInPaise;
  private Long screenId;
  private String screenName;
  private Long theatreId;
  private String theatreName;
  private int availableSeats;

  public static ShowResponse from(Show show, int availableSeats) {
    return ShowResponse.builder()
        .id(show.getId())
        .movieName(show.getMovieName())
        .startTime(show.getStartTime())
        .endTime(show.getEndTime())
        .basePriceInPaise(show.getBasePriceInPaise())
        .screenId(show.getScreenId())
        .screenName(show.getScreenName())
        .theatreId(show.getTheatreId())
        .theatreName(show.getTheatreName())
        .availableSeats(availableSeats)
        .build();
  }
}
