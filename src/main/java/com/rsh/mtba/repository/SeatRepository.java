package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Seat;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
public class SeatRepository {

  private final JdbcTemplate jdbc;

  static final RowMapper<Seat> ROW_MAPPER = (rs, rowNum) -> {
    Seat s = new Seat();
    s.setId(rs.getLong("id"));
    s.setLabel(rs.getString("label"));
    s.setRowNumber(rs.getInt("row_number"));
    s.setColNumber(rs.getInt("col_number"));
    s.setType(Seat.SeatType.valueOf(rs.getString("type")));
    s.setScreenId(rs.getLong("screen_id"));
    return s;
  };

  public void saveAll(List<Seat> seats) {
    for (Seat seat : seats) {
      KeyHolder keys = new GeneratedKeyHolder();
      jdbc.update(con -> {
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO seats (label, row_number, col_number, type, screen_id) VALUES (?,?,?,?,?)",
            new String[]{"id"});
        ps.setString(1, seat.getLabel());
        ps.setInt(2, seat.getRowNumber());
        ps.setInt(3, seat.getColNumber());
        ps.setString(4, seat.getType().name());
        ps.setLong(5, seat.getScreenId());
        return ps;
      }, keys);
      seat.setId(keys.getKey().longValue());
    }
  }

  public List<Seat> findByScreenId(Long screenId) {
    return jdbc.query("SELECT * FROM seats WHERE screen_id=? ORDER BY row_number, col_number",
        ROW_MAPPER, screenId);
  }

  public Optional<Seat> findByScreenIdAndLabel(Long screenId, String label) {
    List<Seat> r = jdbc.query(
        "SELECT * FROM seats WHERE screen_id=? AND label=?", ROW_MAPPER, screenId, label);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM seats");
  }
}
