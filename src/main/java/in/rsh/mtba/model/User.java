package in.rsh.mtba.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {

  private final int userId;
  private final String name;
  private final Gender gender;

  private enum Gender {
    MALE,
    FEMALE
  }
}
