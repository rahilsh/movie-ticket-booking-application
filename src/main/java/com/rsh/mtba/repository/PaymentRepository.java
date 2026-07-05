package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Payment;
import com.rsh.mtba.entity.Payment.PaymentStatus;
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
public class PaymentRepository {

  private final JdbcTemplate jdbc;

  static final RowMapper<Payment> ROW_MAPPER = (rs, rowNum) -> {
    Payment p = new Payment();
    p.setId(rs.getLong("id"));
    p.setTransactionId(rs.getString("transaction_id"));
    p.setBookingId(rs.getLong("booking_id"));
    p.setAmountInPaise(rs.getInt("amount_in_paise"));
    p.setStatus(PaymentStatus.valueOf(rs.getString("status")));
    p.setFailureReason(rs.getString("failure_reason"));
    p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    Timestamp completedAt = rs.getTimestamp("completed_at");
    if (completedAt != null) p.setCompletedAt(completedAt.toLocalDateTime());
    return p;
  };

  public Payment save(Payment payment) {
    return payment.getId() == null ? insert(payment) : update(payment);
  }

  private Payment insert(Payment payment) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          "INSERT INTO payments (transaction_id, booking_id, amount_in_paise, status, created_at) "
          + "VALUES (?,?,?,?,?)",
          new String[]{"id"});
      ps.setString(1, payment.getTransactionId());
      ps.setLong(2, payment.getBookingId());
      ps.setInt(3, payment.getAmountInPaise());
      ps.setString(4, payment.getStatus().name());
      ps.setTimestamp(5, Timestamp.valueOf(payment.getCreatedAt()));
      return ps;
    }, keys);
    payment.setId(keys.getKey().longValue());
    return payment;
  }

  private Payment update(Payment payment) {
    jdbc.update(
        "UPDATE payments SET status=?, failure_reason=?, completed_at=? WHERE id=?",
        payment.getStatus().name(), payment.getFailureReason(),
        payment.getCompletedAt() != null ? Timestamp.valueOf(payment.getCompletedAt()) : null,
        payment.getId());
    return payment;
  }

  public Optional<Payment> findById(Long id) {
    List<Payment> r = jdbc.query("SELECT * FROM payments WHERE id=?", ROW_MAPPER, id);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public Optional<Payment> findByTransactionId(String transactionId) {
    List<Payment> r = jdbc.query(
        "SELECT * FROM payments WHERE transaction_id=?", ROW_MAPPER, transactionId);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public Optional<Payment> findByBookingId(Long bookingId) {
    List<Payment> r = jdbc.query(
        "SELECT * FROM payments WHERE booking_id=?", ROW_MAPPER, bookingId);
    return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
  }

  public void deleteAll() {
    jdbc.update("DELETE FROM payments");
  }
}
