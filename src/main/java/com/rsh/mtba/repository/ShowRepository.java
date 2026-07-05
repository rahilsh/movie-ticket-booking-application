package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Show;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

  List<Show> findByScreenId(Long screenId);

  List<Show> findByMovieNameContainingIgnoreCase(String movieName);

  @Query(
      "SELECT s FROM Show s WHERE s.screen.id = :screenId "
          + "AND s.startTime >= :from AND s.startTime <= :to")
  List<Show> findByScreenIdAndStartTimeBetween(
      @Param("screenId") Long screenId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

  @Query("SELECT s FROM Show s WHERE s.startTime >= :from ORDER BY s.startTime")
  List<Show> findUpcomingShows(@Param("from") LocalDateTime from);
}
