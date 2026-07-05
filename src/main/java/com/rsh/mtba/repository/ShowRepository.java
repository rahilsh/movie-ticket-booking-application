package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Show;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
public class ShowRepository {

  private final JdbcTemplate jdbc;

  // Join query to populate denormalized fields in one shot
  private static final String SELECT_WITH_SCREEN =
      "SELECT sh.*, sc.name AS screen_name, sc.theatre_id, t.name AS theatre_name "
      + "FROM shows sh "
      + "JOIN screens sc ON sc.id = sh.screen_id "
      + "JOIN theatres t  ON t.id  = sc.theatre_id ";

  static final RowMapper<Show> ROW_MAPPER = (rs, rowNum) -> {
    Show s = new Show();
    s.setId(rs.getLong("id"));
    s.setMovieName(rs.getString("movie_name"));
    s.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
    s.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
    s.setBasePriceInPaise(rs.getInt("base_price_in_paise"));
    s.setScreenId(rs.getLong("screen_id"));
    s.setScreenName(rs.getString("screen_name"));
    s.setTheatreId(rs.getLong("theatre_id"));
    s.setTheatreName(rs.getString("theatre_name"));
    return s;
  };

  public Show save(Show show) {
    return show.getId() == null ? insert(show) : update(show);
  }

  private Show insert(Show show) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO shows (movie_name, start_time, end_time, base_price_in_paise, screen_id) "
          + "VALUES (?,?,?,?,?)",
          new String[]{"id"});
      ps.setString(1, show.getMovieName());
      ps.setTimestamp(2, Timestamp.valueOf(show.getStartTime()));
      ps.setTimestamp(3, Timestamp.valueOf(show.getEndTime()));
      ps.setInt(4, show.getBasePriceInPaise());
      ps.setLong(5, show.getScreenId());
      return ps;
    }, keys);
    show.setId(keys.getKey().longValue());
    return show;
  }

  private Show update(Show show) {
    jdbc.update(
        "UPDATE shows SET movie_name=?, start_time=?, end_time=?, base_price_in_paise=? WHERE id=?",
        show.getMovieName(), Timestamp.valueOf(show.getStartTime()),
        Timestamp.valueOf(show.getEndTime()), show.getBasePriceInPaise(), show.getId());
    return show;
  }

  public Optional<Show> findById(Long id) {
    List<Show> r = jdbc.query(SELECT_WITH_SCREEN + "WHERE sh.id=?", ROW_MAPPER, id);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public List<Show> findByScreenId(Long screenId) {
    return jdbc.query(SELECT_WITH_SCREEN + "WHERE sh.screen_id=? ORDER BY sh.start_time",
        ROW_MAPPER, screenId);
  }

  public List<Show> findUpcomingShows(LocalDateTime from) {
    return jdbc.query(SELECT_WITH_SCREEN + "WHERE sh.start_time >= ? ORDER BY sh.start_time",
        ROW_MAPPER, Timestamp.valueOf(from));
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM shows");
  }
}
