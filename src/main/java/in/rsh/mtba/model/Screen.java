package in.rsh.mtba.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class Screen {

  private int screenId;
  private int theatreId;
  private final String name;
  private int totalCapacity;
  private int length;
  private int breadth;
  private final String[][] seatLayout;
}
