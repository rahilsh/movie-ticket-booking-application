package in.rsh.mtba.store;

import in.rsh.mtba.model.Booking;
import in.rsh.mtba.model.Payment;
import in.rsh.mtba.model.Screen;
import in.rsh.mtba.model.Show;
import in.rsh.mtba.model.ShowSeat;
import in.rsh.mtba.model.Theatre;
import in.rsh.mtba.model.User;
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
