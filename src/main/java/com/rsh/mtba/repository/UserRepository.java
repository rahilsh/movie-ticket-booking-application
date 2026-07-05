package com.rsh.mtba.repository;

import com.rsh.mtba.entity.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
@RequiredArgsConstructor
public class UserRepository {

  private final JdbcTemplate jdbc;

  private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

  private static User mapRow(ResultSet rs) throws SQLException {
    User u = new User();
    u.setId(rs.getLong("id"));
    u.setName(rs.getString("name"));
    u.setEmail(rs.getString("email"));
    u.setPasswordHash(rs.getString("password_hash"));
    u.setPhone(rs.getString("phone"));
    u.setGender(User.Gender.valueOf(rs.getString("gender")));
    u.setRole(User.Role.valueOf(rs.getString("role")));
    Timestamp ts = rs.getTimestamp("created_at");
    if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
    return u;
  }

  public User save(User user) {
    if (user.getId() == null) {
      return insert(user);
    }
    return update(user);
  }

  private User insert(User user) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO users (name, email, password_hash, phone, gender, role, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)",
          new String[]{"id"});
      ps.setString(1, user.getName());
      ps.setString(2, user.getEmail());
      ps.setString(3, user.getPasswordHash());
      ps.setString(4, user.getPhone());
      ps.setString(5, user.getGender().name());
      ps.setString(6, user.getRole().name());
      ps.setTimestamp(7, Timestamp.valueOf(user.getCreatedAt()));
      return ps;
    }, keys);
    user.setId(keys.getKey().longValue());
    return user;
  }

  private User update(User user) {
    jdbc.update(
        "UPDATE users SET name=?, email=?, password_hash=?, phone=?, gender=?, role=? WHERE id=?",
        user.getName(), user.getEmail(), user.getPasswordHash(), user.getPhone(),
        user.getGender().name(), user.getRole().name(), user.getId());
    return user;
  }

  public Optional<User> findById(Long id) {
    List<User> results = jdbc.query("SELECT * FROM users WHERE id=?", ROW_MAPPER, id);
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }

  public Optional<User> findByEmail(String email) {
    List<User> results = jdbc.query("SELECT * FROM users WHERE email=?", ROW_MAPPER, email);
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }

  public boolean existsByEmail(String email) {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM users WHERE email=?", Integer.class, email);
    return count != null && count > 0;
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM users");
  }
}
