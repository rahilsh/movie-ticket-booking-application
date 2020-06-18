package in.rsh.mtba.store;

import static org.junit.Assert.assertEquals;

import in.rsh.mtba.model.Theatre;
import org.junit.Test;

public class StoreFactoryTest {

  @Test
  public void testStoreFactory() {
    GenericStore<Theatre> store = StoreFactory.getInstance().getStore(Theatre.class);
    Theatre theatre = Theatre.builder().theatreId(1).build();
    store.put(theatre.getTheatreId(), theatre);
    assertEquals(theatre, store.get(theatre.getTheatreId()));
  }
}
