package in.rsh.mtba.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class Show {
  private final int showId;
  private final int screenId;
  private final LocalDateTime startTime;
  private final LocalDateTime endTime;
}
