package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Seat;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
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
public class ShowSeatRepository {

  private final JdbcTemplate jdbc;

  // Join to seats so we can populate label/row/col/type in one query
  private static final String SELECT_WITH_SEAT =
      "SELECT ss.id, ss.show_id, ss.seat_id, ss.status, ss.version, "
      + "se.label AS seat_label, se.row_number, se.col_number, se.type AS seat_type "
      + "FROM show_seats ss "
      + "JOIN seats se ON se.id = ss.seat_id ";

  static final RowMapper<ShowSeat> ROW_MAPPER = (rs, rowNum) -> {
    ShowSeat s = new ShowSeat();
    s.setId(rs.getLong("id"));
    s.setShowId(rs.getLong("show_id"));
    s.setSeatId(rs.getLong("seat_id"));
    s.setStatus(ShowSeatStatus.valueOf(rs.getString("status")));
    s.setVersion(rs.getLong("version"));
    s.setSeatLabel(rs.getString("seat_label"));
    s.setRowNumber(rs.getInt("row_number"));
    s.setColNumber(rs.getInt("col_number"));
    s.setSeatType(Seat.SeatType.valueOf(rs.getString("seat_type")));
    return s;
  };

  public void saveAll(List<ShowSeat> showSeats) {
    for (ShowSeat ss : showSeats) {
      if (ss.getId() == null) {
        insert(ss);
      } else {
        updateStatus(ss);
      }
    }
  }

  private void insert(ShowSeat ss) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO show_seats (show_id, seat_id, status, version) VALUES (?,?,?,0)",
          new String[]{"id"});
      ps.setLong(1, ss.getShowId());
      ps.setLong(2, ss.getSeatId());
      ps.setString(3, ss.getStatus().name());
      return ps;
    }, keys);
    ss.setId(keys.getKey().longValue());
    ss.setVersion(0L);
  }

  /**
   * Optimistic-lock-safe update: only succeeds when version matches.
   * Throws if another transaction already changed this row (version mismatch = 0 rows updated).
   */
  private void updateStatus(ShowSeat ss) {
    int updated = jdbc.update(
        "UPDATE show_seats SET status=?, version=version+1 WHERE id=? AND version=?",
        ss.getStatus().name(), ss.getId(), ss.getVersion());
    if (updated == 0) {
      throw new org.springframework.dao.OptimisticLockingFailureException(
          "ShowSeat id=" + ss.getId() + " was modified by another transaction");
    }
    ss.setVersion(ss.getVersion() + 1);
  }

  public Optional<ShowSeat> findById(Long id) {
    List<ShowSeat> r = jdbc.query(SELECT_WITH_SEAT + "WHERE ss.id=?", ROW_MAPPER, id);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public List<ShowSeat> findByShowId(Long showId) {
    return jdbc.query(SELECT_WITH_SEAT + "WHERE ss.show_id=? ORDER BY se.row_number, se.col_number",
        ROW_MAPPER, showId);
  }

  public List<ShowSeat> findByShowIdAndStatus(Long showId, ShowSeatStatus status) {
    return jdbc.query(SELECT_WITH_SEAT + "WHERE ss.show_id=? AND ss.status=?",
        ROW_MAPPER, showId, status.name());
  }

  /**
   * Fetches the requested seats with a SELECT ... FOR UPDATE (pessimistic write lock).
   * Prevents two concurrent transactions from booking the same seats.
   */
  public List<ShowSeat> findByShowIdAndSeatLabelInWithLock(Long showId, List<String> labels) {
    if (labels == null || labels.isEmpty()) return Collections.emptyList();
    String placeholders = String.join(",", Collections.nCopies(labels.size(), "?"));
    String sql = "SELECT ss.id, ss.show_id, ss.seat_id, ss.status, ss.version, "
        + "se.label AS seat_label, se.row_number, se.col_number, se.type AS seat_type "
        + "FROM show_seats ss "
        + "JOIN seats se ON se.id = ss.seat_id "
        + "WHERE ss.show_id=? AND se.label IN (" + placeholders + ") "
        + "FOR UPDATE";

    Object[] params = new Object[1 + labels.size()];
    params[0] = showId;
    for (int i = 0; i < labels.size(); i++) params[i + 1] = labels.get(i);

    return jdbc.query(sql, ROW_MAPPER, params);
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM show_seats");
  }
}
