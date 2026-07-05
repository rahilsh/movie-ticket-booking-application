package com.rsh.mtba.repository;

import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

  List<ShowSeat> findByShowId(Long showId);

  List<ShowSeat> findByShowIdAndStatus(Long showId, ShowSeatStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.id IN :ids")
  List<ShowSeat> findByShowIdAndIdInWithLock(
      @Param("showId") Long showId, @Param("ids") List<Long> ids);

  @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.seat.label IN :labels")
  List<ShowSeat> findByShowIdAndSeatLabelIn(
      @Param("showId") Long showId, @Param("labels") List<String> labels);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.seat.label IN :labels")
  List<ShowSeat> findByShowIdAndSeatLabelInWithLock(
      @Param("showId") Long showId, @Param("labels") List<String> labels);
}
