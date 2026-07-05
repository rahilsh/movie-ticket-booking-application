package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Theatre;
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
public class TheatreRepository {

  private final JdbcTemplate jdbc;

  private static final RowMapper<Theatre> ROW_MAPPER = (rs, rowNum) -> {
    Theatre t = new Theatre();
    t.setId(rs.getLong("id"));
    t.setName(rs.getString("name"));
    t.setAddress(rs.getString("address"));
    t.setCity(rs.getString("city"));
    return t;
  };

  public Theatre save(Theatre theatre) {
    if (theatre.getId() == null) {
      return insert(theatre);
    }
    return update(theatre);
  }

  private Theatre insert(Theatre theatre) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO theatres (name, address, city) VALUES (?, ?, ?)",
          new String[]{"id"});
      ps.setString(1, theatre.getName());
      ps.setString(2, theatre.getAddress());
      ps.setString(3, theatre.getCity());
      return ps;
    }, keys);
    theatre.setId(keys.getKey().longValue());
    return theatre;
  }

  private Theatre update(Theatre theatre) {
    jdbc.update("UPDATE theatres SET name=?, address=?, city=? WHERE id=?",
        theatre.getName(), theatre.getAddress(), theatre.getCity(), theatre.getId());
    return theatre;
  }

  public Optional<Theatre> findById(Long id) {
    List<Theatre> r = jdbc.query("SELECT * FROM theatres WHERE id=?", ROW_MAPPER, id);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public List<Theatre> findAll() {
    return jdbc.query("SELECT * FROM theatres ORDER BY name", ROW_MAPPER);
  }

  public List<Theatre> findByCity(String city) {
    return jdbc.query("SELECT * FROM theatres WHERE city=? ORDER BY name", ROW_MAPPER, city);
  }

  public void delete(Long id) {
    jdbc.update("DELETE FROM theatres WHERE id=?", id);
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM theatres");
  }
}
