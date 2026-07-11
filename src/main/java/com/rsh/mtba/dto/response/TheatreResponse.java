package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.Theatre;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreResponse {
  private Long id;
  private String name;
  private String address;
  private String city;

  public static TheatreResponse from(Theatre theatre) {
    return TheatreResponse.builder()
        .id(theatre.getId())
        .name(theatre.getName())
        .address(theatre.getAddress())
        .city(theatre.getCity())
        .build();
  }
}
