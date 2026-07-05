package com.rsh.mtba.repository;

import com.rsh.mtba.entity.Theatre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

  List<Theatre> findByCity(String city);

  List<Theatre> findByNameContainingIgnoreCase(String name);
}
