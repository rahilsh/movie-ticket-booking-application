package com.rsh.mtba.entity;

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
public class Screen {

  private Long id;
  private String name;
  private int totalCapacity;
  private int rows;
  private int cols;
  private Long theatreId;
  // Denormalized for response convenience — populated by join queries
  private String theatreName;
}
