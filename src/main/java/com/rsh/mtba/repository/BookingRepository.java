package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookingRepository {

  private final JdbcTemplate jdbc;

  // Base mapper — does not include seatLabels (populated separately via join)
  static final RowMapper<Booking> ROW_MAPPER = (rs, rowNum) -> {
    Booking b = new Booking();
    b.setId(rs.getLong("id"));
    b.setUserId(rs.getLong("user_id"));
    b.setShowId(rs.getLong("show_id"));
    b.setTotalAmountInPaise(rs.getInt("total_amount_in_paise"));
    b.setStatus(BookingStatus.valueOf(rs.getString("status")));
    b.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    Timestamp upd = rs.getTimestamp("updated_at");
    if (upd != null) b.setUpdatedAt(upd.toLocalDateTime());
    b.setMovieName(rs.getString("movie_name"));
    return b;
  };

  private static final String SELECT_WITH_SHOW =
      "SELECT b.*, sh.movie_name FROM bookings b "
      + "JOIN shows sh ON sh.id = b.show_id ";

  public Booking save(Booking booking) {
    if (booking.getId() == null) {
      return insert(booking);
    }
    return update(booking);
  }

  private Booking insert(Booking booking) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO bookings (user_id, show_id, total_amount_in_paise, status, created_at) "
          + "VALUES (?,?,?,?,?)",
          new String[]{"id"});
      ps.setLong(1, booking.getUserId());
      ps.setLong(2, booking.getShowId());
      ps.setInt(3, booking.getTotalAmountInPaise());
      ps.setString(4, booking.getStatus().name());
      ps.setTimestamp(5, Timestamp.valueOf(booking.getCreatedAt()));
      return ps;
    }, keys);
    booking.setId(keys.getKey().longValue());
    return booking;
  }

  private Booking update(Booking booking) {
    jdbc.update(
        "UPDATE bookings SET status=?, updated_at=NOW() WHERE id=?",
        booking.getStatus().name(), booking.getId());
    return booking;
  }

  public Optional<Booking> findById(Long id) {
    List<Booking> r = jdbc.query(SELECT_WITH_SHOW + "WHERE b.id=?", ROW_MAPPER, id);
    if (r.isEmpty()) return Optional.empty();
    Booking b = r.get(0);
    b.setSeatLabels(findSeatLabels(id));
    return Optional.of(b);
  }

  public Optional<Booking> findByIdWithLock(Long id) {
    List<Booking> r = jdbc.query(
        SELECT_WITH_SHOW + "WHERE b.id=? FOR UPDATE OF b", ROW_MAPPER, id);
    if (r.isEmpty()) return Optional.empty();
    Booking b = r.get(0);
    b.setSeatLabels(findSeatLabels(id));
    return Optional.of(b);
  }

  public List<Booking> findByUserId(Long userId) {
    List<Booking> bookings = jdbc.query(
        SELECT_WITH_SHOW + "WHERE b.user_id=? ORDER BY b.created_at DESC", ROW_MAPPER, userId);
    bookings.forEach(b -> b.setSeatLabels(findSeatLabels(b.getId())));
    return bookings;
  }

  /** Returns seat labels booked under a given booking id. */
  private List<String> findSeatLabels(Long bookingId) {
    return jdbc.queryForList(
        "SELECT se.label FROM booking_show_seats bss "
        + "JOIN show_seats ss ON ss.id = bss.show_seat_id "
        + "JOIN seats se ON se.id = ss.seat_id "
        + "WHERE bss.booking_id=? ORDER BY se.label",
        String.class, bookingId);
  }

  /** Links a booking to the given show seat ids in the join table. */
  public void saveBookingSeats(Long bookingId, List<Long> showSeatIds) {
    for (Long seatId : showSeatIds) {
      jdbc.update(
          "INSERT INTO booking_show_seats (booking_id, show_seat_id) VALUES (?,?)",
          bookingId, seatId);
    }
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM bookings");
  }
}
