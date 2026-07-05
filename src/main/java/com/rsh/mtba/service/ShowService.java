package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.ShowRequest;
import com.rsh.mtba.dto.response.ShowResponse;
import com.rsh.mtba.dto.response.ShowSeatResponse;
import com.rsh.mtba.entity.Screen;
import com.rsh.mtba.entity.Seat;
import com.rsh.mtba.entity.Show;
import com.rsh.mtba.entity.ShowSeat;
import com.rsh.mtba.entity.ShowSeat.ShowSeatStatus;
import com.rsh.mtba.exception.BookingException;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.SeatRepository;
import com.rsh.mtba.repository.ShowRepository;
import com.rsh.mtba.repository.ShowSeatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowService {

  private final ShowRepository showRepository;
  private final ShowSeatRepository showSeatRepository;
  private final SeatRepository seatRepository;
  private final ScreenService screenService;

  @Transactional
  public ShowResponse create(Long screenId, ShowRequest request) {
    Screen screen = screenService.findById(screenId);

    if (!request.getEndTime().isAfter(request.getStartTime())) {
      throw new BookingException("End time must be after start time");
    }

    Show show = Show.builder()
        .movieName(request.getMovieName())
        .startTime(request.getStartTime())
        .endTime(request.getEndTime())
        .basePriceInPaise(request.getBasePriceInPaise())
        .screenId(screen.getId())
        .screenName(screen.getName())
        .theatreId(screen.getTheatreId())
        .theatreName(screen.getTheatreName())
        .build();
    Show saved = showRepository.save(show);
    initShowSeats(saved, screen);

    int available = showSeatRepository.findByShowIdAndStatus(saved.getId(), ShowSeatStatus.AVAILABLE).size();
    log.info("Created show id={} movie='{}' screenId={}", saved.getId(), saved.getMovieName(), screenId);
    return ShowResponse.from(saved, available);
  }

  private void initShowSeats(Show show, Screen screen) {
    List<Seat> seats = seatRepository.findByScreenId(screen.getId());
    List<ShowSeat> showSeats = seats.stream()
        .filter(seat -> seat.getType() != Seat.SeatType.BLOCKED)
        .map(seat -> ShowSeat.builder()
            .showId(show.getId())
            .seatId(seat.getId())
            .seatLabel(seat.getLabel())
            .rowNumber(seat.getRowNumber())
            .colNumber(seat.getColNumber())
            .seatType(seat.getType())
            .status(ShowSeatStatus.AVAILABLE)
            .build())
        .collect(Collectors.toList());
    showSeatRepository.saveAll(showSeats);
    log.debug("Initialized {} show seats for showId={}", showSeats.size(), show.getId());
  }

  public ShowResponse getById(Long id) {
    Show show = findById(id);
    int available = showSeatRepository.findByShowIdAndStatus(id, ShowSeatStatus.AVAILABLE).size();
    return ShowResponse.from(show, available);
  }

  public List<ShowResponse> getByScreen(Long screenId) {
    screenService.findById(screenId);
    return showRepository.findByScreenId(screenId).stream()
        .map(show -> {
          int available = showSeatRepository
              .findByShowIdAndStatus(show.getId(), ShowSeatStatus.AVAILABLE).size();
          return ShowResponse.from(show, available);
        })
        .collect(Collectors.toList());
  }

  public List<ShowResponse> getUpcoming() {
    return showRepository.findUpcomingShows(LocalDateTime.now()).stream()
        .map(show -> {
          int available = showSeatRepository
              .findByShowIdAndStatus(show.getId(), ShowSeatStatus.AVAILABLE).size();
          return ShowResponse.from(show, available);
        })
        .collect(Collectors.toList());
  }

  public List<ShowSeatResponse> getSeats(Long showId) {
    findById(showId);
    return showSeatRepository.findByShowId(showId).stream()
        .map(ShowSeatResponse::from)
        .collect(Collectors.toList());
  }

  public Show findById(Long id) {
    return showRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Show", id));
  }
}
