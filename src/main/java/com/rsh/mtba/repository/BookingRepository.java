package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Booking;
import com.rsh.mtba.entity.Booking.BookingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

  List<Booking> findByUserId(Long userId);

  List<Booking> findByShowId(Long showId);

  List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);
}
