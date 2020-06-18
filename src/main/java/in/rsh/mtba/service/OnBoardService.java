package in.rsh.mtba.service;

import in.rsh.mtba.model.Screen;
import in.rsh.mtba.model.Show;
import in.rsh.mtba.model.ShowSeat;
import in.rsh.mtba.model.Theatre;
import in.rsh.mtba.store.GenericStore;
import in.rsh.mtba.store.StoreFactory;
import in.rsh.mtba.model.ShowSeat.ShowSeatStatus;
import org.apache.commons.lang3.RandomStringUtils;

public class OnBoardService {

  private final GenericStore<Theatre> theatreStore =
      StoreFactory.getInstance().getStore(Theatre.class);
  private final GenericStore<Screen> screenStore =
      StoreFactory.getInstance().getStore(Screen.class);
  //TODO: use ShowService instead of ShowStore
  private final GenericStore<Show> showStore = StoreFactory.getInstance().getStore(Show.class);

  private final GenericStore<ShowSeat> showSeatsStore =
      StoreFactory.getInstance().getStore(ShowSeat.class);

  public Theatre addTheatre(Theatre theatre) {
    int theatreId = Integer.parseInt(RandomStringUtils.randomNumeric(7));
    Theatre updatedTheatre = theatre.toBuilder().theatreId(theatreId).build();
    theatreStore.put(theatreId, updatedTheatre);
    return updatedTheatre;
  }

  public Screen addScreen(Screen screen) {
    int screenId = Integer.parseInt(RandomStringUtils.randomNumeric(7));
    Screen updateScreen = screen.toBuilder().screenId(screenId).build();
    screenStore.put(screenId, updateScreen);
    return updateScreen;
  }

  public Show addShow(Show show) {
    int showId = Integer.parseInt(RandomStringUtils.randomNumeric(7));
    Show updatedShow = show.toBuilder().showId(showId).build();
    showStore.put(showId, updatedShow);
    Screen screen = screenStore.get(show.getScreenId());
    initShowSeats(showId, screen);
    return updatedShow;
  }

  private void initShowSeats(int showId, Screen screen) {
    String[][] seatLayout = screen.getSeatLayout();
    for (String[] row : seatLayout) {
      for (String seat : row) {
        if (!seat.equals("-1") && !seat.equals("0") && !seat.equals("-2")) {
          showSeatsStore.put(
              seat, ShowSeat.builder().seatId(seat).showId(showId).status(ShowSeatStatus.AVAILABLE).build());
        }
      }
    }
  }
}
