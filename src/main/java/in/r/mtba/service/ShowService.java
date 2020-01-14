package in.r.mtba.service;

import in.r.mtba.model.Show;
import in.r.mtba.model.ShowSeat;
import in.r.mtba.store.GenericStore;
import in.r.mtba.store.StoreFactory;
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
}
