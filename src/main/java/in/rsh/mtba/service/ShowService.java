package in.rsh.mtba.service;

import in.rsh.mtba.model.Show;
import in.rsh.mtba.model.ShowSeat;
import in.rsh.mtba.store.GenericStore;
import in.rsh.mtba.store.StoreFactory;
import java.util.List;
import java.util.stream.Collectors;

public class ShowService {
  private final GenericStore<Show> showStore = StoreFactory.getInstance().getStore(Show.class);
  private final GenericStore<ShowSeat> showSeatStore =
      StoreFactory.getInstance().getStore(ShowSeat.class);

  public List<Show> getAllShows(int screenId) {
    return showStore.getAll().stream()
        .filter(show -> show.getScreenId() == screenId)
        .collect(Collectors.toList());
  }

  public List<ShowSeat> getSeatsForShows(int showId) {
    return showSeatStore.getAll().stream()
        .filter(showSeat -> showSeat.getShowId() == showId)
        .collect(Collectors.toList());
  }

  public List<ShowSeat> getSeats() {
    return showSeatStore.getAll();
  }
}
