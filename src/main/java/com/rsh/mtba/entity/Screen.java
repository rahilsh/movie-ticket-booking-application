package com.rsh.mtba.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "screens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @Min(1)
  @Column(nullable = false)
  private int totalCapacity;

  @Min(1)
  @Column(nullable = false)
  private int rows;

  @Min(1)
  @Column(nullable = false)
  private int cols;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "theatre_id", nullable = false)
  private Theatre theatre;

  @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Seat> seats;

  @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Show> shows;
}
