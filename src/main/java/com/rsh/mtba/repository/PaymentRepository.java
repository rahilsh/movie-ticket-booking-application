package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByBookingId(Long bookingId);

  Optional<Payment> findByTransactionId(String transactionId);
}
