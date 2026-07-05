package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Seat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

  List<Seat> findByScreenId(Long screenId);

  Optional<Seat> findByScreenIdAndLabel(Long screenId, String label);
}
