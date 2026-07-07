package com.rsh.mtba.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes all test data in FK-safe dependency order (children before parents).
 * Use in @BeforeEach / @BeforeAll to guarantee a clean slate regardless of
 * which other test classes ran before in the shared Spring context.
 *
 * Deletion order:
 *   payments
 *   booking_show_seats  (join table)
 *   bookings
 *   show_seats
 *   shows
 *   seats
 *   screens
 *   theatres
 *   users
 */
@Component
@RequiredArgsConstructor
public class TestDataCleaner {

  private final JdbcTemplate jdbc;

  @Transactional
  public void clean() {
    jdbc.update("DELETE FROM payments");
    jdbc.update("DELETE FROM booking_show_seats");
    jdbc.update("DELETE FROM bookings");
    jdbc.update("DELETE FROM show_seats");
    jdbc.update("DELETE FROM shows");
    jdbc.update("DELETE FROM seats");
    jdbc.update("DELETE FROM screens");
    jdbc.update("DELETE FROM theatres");
    jdbc.update("DELETE FROM users");
  }
}
