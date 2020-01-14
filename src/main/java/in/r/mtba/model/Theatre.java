package in.r.mtba.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class Theatre {
  private final int theatreId;
  private final String name;
  private final String address;
}
