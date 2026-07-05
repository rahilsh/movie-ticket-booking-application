package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Screen;
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
public class ScreenRepository {

  private final JdbcTemplate jdbc;

  static final RowMapper<Screen> ROW_MAPPER = (rs, rowNum) -> {
    Screen s = new Screen();
    s.setId(rs.getLong("id"));
    s.setName(rs.getString("name"));
    s.setTotalCapacity(rs.getInt("total_capacity"));
    s.setRows(rs.getInt("rows"));
    s.setCols(rs.getInt("cols"));
    s.setTheatreId(rs.getLong("theatre_id"));
    s.setTheatreName(rs.getString("theatre_name"));
    return s;
  };

  private static final String SELECT_WITH_THEATRE =
      "SELECT s.*, t.name AS theatre_name FROM screens s "
      + "JOIN theatres t ON t.id = s.theatre_id ";

  public Screen save(Screen screen) {
    return screen.getId() == null ? insert(screen) : update(screen);
  }

  private Screen insert(Screen screen) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO screens (name, total_capacity, rows, cols, theatre_id) VALUES (?,?,?,?,?)",
          new String[]{"id"});
      ps.setString(1, screen.getName());
      ps.setInt(2, screen.getTotalCapacity());
      ps.setInt(3, screen.getRows());
      ps.setInt(4, screen.getCols());
      ps.setLong(5, screen.getTheatreId());
      return ps;
    }, keys);
    screen.setId(keys.getKey().longValue());
    return screen;
  }

  private Screen update(Screen screen) {
    jdbc.update("UPDATE screens SET name=?, total_capacity=?, rows=?, cols=?, theatre_id=? WHERE id=?",
        screen.getName(), screen.getTotalCapacity(), screen.getRows(), screen.getCols(),
        screen.getTheatreId(), screen.getId());
    return screen;
  }

  public Optional<Screen> findById(Long id) {
    List<Screen> r = jdbc.query(SELECT_WITH_THEATRE + "WHERE s.id=?", ROW_MAPPER, id);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public List<Screen> findByTheatreId(Long theatreId) {
    return jdbc.query(SELECT_WITH_THEATRE + "WHERE s.theatre_id=? ORDER BY s.name",
        ROW_MAPPER, theatreId);
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM screens");
  }
}
