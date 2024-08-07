package in.rsh.mtba.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import in.rsh.mtba.model.Theatre;
import org.junit.jupiter.api.Test;

class StoreFactoryTest {

  @Test
  void storeFactory() {
    GenericStore<Theatre> store = StoreFactory.getInstance().getStore(Theatre.class);
    Theatre theatre = Theatre.builder().theatreId(1).build();
    store.put(theatre.getTheatreId(), theatre);
    assertEquals(theatre, store.get(theatre.getTheatreId()));
  }
}
