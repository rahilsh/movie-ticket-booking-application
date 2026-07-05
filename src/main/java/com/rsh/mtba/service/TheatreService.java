package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.entity.Theatre;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.TheatreRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TheatreService {

  private final TheatreRepository theatreRepository;

  @Transactional
  public TheatreResponse create(TheatreRequest request) {
    Theatre theatre = Theatre.builder()
        .name(request.getName())
        .address(request.getAddress())
        .city(request.getCity())
        .build();
    Theatre saved = theatreRepository.save(theatre);
    log.info("Created theatre id={} name={}", saved.getId(), saved.getName());
    return TheatreResponse.from(saved);
  }

  public TheatreResponse getById(Long id) {
    return TheatreResponse.from(findById(id));
  }

  public List<TheatreResponse> getAll() {
    return theatreRepository.findAll().stream()
        .map(TheatreResponse::from)
        .collect(Collectors.toList());
  }

  public List<TheatreResponse> getByCity(String city) {
    return theatreRepository.findByCity(city).stream()
        .map(TheatreResponse::from)
        .collect(Collectors.toList());
  }

  public Theatre findById(Long id) {
    return theatreRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Theatre", id));
  }

  @Transactional
  public TheatreResponse update(Long id, TheatreRequest request) {
    Theatre theatre = findById(id);
    theatre.setName(request.getName());
    theatre.setAddress(request.getAddress());
    theatre.setCity(request.getCity());
    return TheatreResponse.from(theatreRepository.save(theatre));
  }

  @Transactional
  public void delete(Long id) {
    findById(id); // validate exists
    theatreRepository.delete(id);
    log.info("Deleted theatre id={}", id);
  }
}
