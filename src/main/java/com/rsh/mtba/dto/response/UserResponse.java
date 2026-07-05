package com.rsh.mtba.dto.response;

import com.rsh.mtba.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
  private Long id;
  private String name;
  private String email;
  private String phone;
  private User.Gender gender;
  private User.Role role;

  public static UserResponse from(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .phone(user.getPhone())
        .gender(user.getGender())
        .role(user.getRole())
        .build();
  }
}
