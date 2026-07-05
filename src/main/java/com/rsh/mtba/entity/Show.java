package com.rsh.mtba.entity;

import java.time.LocalDateTime;
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
public class Show {

  private Long id;
  private String movieName;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private int basePriceInPaise;
  private Long screenId;
  // Denormalized for response convenience — populated by join queries
  private String screenName;
  private Long theatreId;
  private String theatreName;
}
