package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Screen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

  List<Screen> findByTheatreId(Long theatreId);
}
