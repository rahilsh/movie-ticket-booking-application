package in.r.mtba.store;

import in.r.mtba.model.Booking;
import in.r.mtba.model.Payment;
import in.r.mtba.model.Screen;
import in.r.mtba.model.Show;
import in.r.mtba.model.ShowSeat;
import in.r.mtba.model.Theatre;
import in.r.mtba.model.User;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class StoreFactory {
  Map<Type, GenericStore> storeMapping = new HashMap<>();

  private static StoreFactory storeFactory = null;

  public static StoreFactory getInstance() {
    if (storeFactory == null) storeFactory = new StoreFactory();
    return storeFactory;
  }

  private StoreFactory() {
    storeMapping.put(Theatre.class, new GenericStore<Theatre>());
    storeMapping.put(Screen.class, new GenericStore<Screen>());
    storeMapping.put(Show.class, new GenericStore<Show>());
    storeMapping.put(User.class, new GenericStore<User>());
    storeMapping.put(Payment.class, new GenericStore<Payment>());
    storeMapping.put(Booking.class, new GenericStore<Booking>());
    storeMapping.put(ShowSeat.class, new GenericStore<ShowSeat>());
  }

  public <T> GenericStore<T> getStore(Type t) {
    return (GenericStore<T>) storeMapping.get(t);
  }
}
