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
public class User {

  private Long id;
  private String name;
  private String email;
  private String passwordHash;
  private String phone;
  private Gender gender;

  @Builder.Default
  private Role role = Role.ROLE_USER;

  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  public enum Gender {
    MALE, FEMALE, OTHER
  }

  public enum Role {
    ROLE_USER, ROLE_ADMIN
  }
}
