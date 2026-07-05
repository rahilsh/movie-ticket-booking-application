package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.ScreenRequest;
import com.rsh.mtba.dto.response.ScreenResponse;
import com.rsh.mtba.entity.Screen;
import com.rsh.mtba.entity.Seat;
import com.rsh.mtba.entity.Theatre;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.ScreenRepository;
import com.rsh.mtba.repository.SeatRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenService {

  private final ScreenRepository screenRepository;
  private final SeatRepository seatRepository;
  private final TheatreService theatreService;

  @Transactional
  public ScreenResponse create(Long theatreId, ScreenRequest request) {
    Theatre theatre = theatreService.findById(theatreId);
    Screen screen = Screen.builder()
        .name(request.getName())
        .rows(request.getRows())
        .cols(request.getCols())
        .totalCapacity(request.getRows() * request.getCols())
        .theatreId(theatre.getId())
        .theatreName(theatre.getName())
        .build();
    Screen saved = screenRepository.save(screen);
    initSeats(saved, request.getRows(), request.getCols());
    log.info("Created screen id={} name={} for theatreId={}", saved.getId(), saved.getName(), theatreId);
    return ScreenResponse.from(saved);
  }

  private void initSeats(Screen screen, int rows, int cols) {
    List<Seat> seats = new ArrayList<>();
    for (int r = 0; r < rows; r++) {
      char rowChar = (char) ('A' + r);
      for (int c = 1; c <= cols; c++) {
        seats.add(Seat.builder()
            .label(rowChar + String.valueOf(c))
            .rowNumber(r)
            .colNumber(c - 1)
            .type(Seat.SeatType.REGULAR)
            .screenId(screen.getId())
            .build());
      }
    }
    seatRepository.saveAll(seats);
    log.debug("Initialized {} seats for screenId={}", seats.size(), screen.getId());
  }

  public ScreenResponse getById(Long id) {
    return ScreenResponse.from(findById(id));
  }

  public List<ScreenResponse> getByTheatre(Long theatreId) {
    theatreService.findById(theatreId);
    return screenRepository.findByTheatreId(theatreId).stream()
        .map(ScreenResponse::from)
        .collect(Collectors.toList());
  }

  public Screen findById(Long id) {
    return screenRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Screen", id));
  }
}
