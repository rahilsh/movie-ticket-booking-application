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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

  @Mock private ShowRepository showRepository;
  @Mock private ShowSeatRepository showSeatRepository;
  @Mock private SeatRepository seatRepository;
  @Mock private ScreenService screenService;

  @InjectMocks private ShowService showService;

  private Screen screen;
  private Show show;
  private Seat seat1, seat2;
  private ShowSeat ss1, ss2;

  @BeforeEach
  void setUp() {
    screen = Screen.builder().id(1L).name("S1").rows(2).cols(2).totalCapacity(4)
        .theatreId(1L).theatreName("PVR").build();
    show = Show.builder().id(1L).movieName("Dune").screenId(1L).screenName("S1")
        .theatreId(1L).theatreName("PVR")
        .startTime(LocalDateTime.now().plusDays(1))
        .endTime(LocalDateTime.now().plusDays(1).plusHours(3))
        .basePriceInPaise(30000).build();
    seat1 = Seat.builder().id(1L).label("A1").rowNumber(0).colNumber(0)
        .type(Seat.SeatType.REGULAR).screenId(1L).build();
    seat2 = Seat.builder().id(2L).label("A2").rowNumber(0).colNumber(1)
        .type(Seat.SeatType.REGULAR).screenId(1L).build();
    ss1 = ShowSeat.builder().id(1L).showId(1L).seatId(1L).seatLabel("A1")
        .status(ShowSeatStatus.AVAILABLE).version(0L).build();
    ss2 = ShowSeat.builder().id(2L).showId(1L).seatId(2L).seatLabel("A2")
        .status(ShowSeatStatus.AVAILABLE).version(0L).build();
  }

  @Test
  @DisplayName("create() throws BookingException when endTime is before startTime")
  void create_invalidTimes_throws() {
    ShowRequest req = new ShowRequest();
    req.setMovieName("Bad");
    req.setStartTime(LocalDateTime.now().plusDays(1));
    req.setEndTime(LocalDateTime.now());
    req.setBasePriceInPaise(1000);
    when(screenService.findById(1L)).thenReturn(screen);

    assertThatThrownBy(() -> showService.create(1L, req))
        .isInstanceOf(BookingException.class)
        .hasMessageContaining("End time");
  }

  @Test
  @DisplayName("create() seeds ShowSeats for all non-BLOCKED seats")
  void create_seedsShowSeats() {
    ShowRequest req = new ShowRequest();
    req.setMovieName("Dune");
    req.setStartTime(LocalDateTime.now().plusDays(1));
    req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
    req.setBasePriceInPaise(30000);

    when(screenService.findById(1L)).thenReturn(screen);
    when(showRepository.save(any())).thenReturn(show);
    when(seatRepository.findByScreenId(1L)).thenReturn(List.of(seat1, seat2));
    when(showSeatRepository.findByShowIdAndStatus(1L, ShowSeatStatus.AVAILABLE))
        .thenReturn(List.of(ss1, ss2));

    ShowResponse response = showService.create(1L, req);

    verify(showSeatRepository).saveAll(anyList());
    assertThat(response.getAvailableSeats()).isEqualTo(2);
  }

  @Test
  @DisplayName("getById() returns show with available seat count")
  void getById_success() {
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(showSeatRepository.findByShowIdAndStatus(1L, ShowSeatStatus.AVAILABLE))
        .thenReturn(List.of(ss1));

    ShowResponse response = showService.getById(1L);
    assertThat(response.getMovieName()).isEqualTo("Dune");
    assertThat(response.getAvailableSeats()).isEqualTo(1);
  }

  @Test
  @DisplayName("getById() throws ResourceNotFoundException for unknown show")
  void getById_notFound() {
    when(showRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> showService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getByScreen() returns shows for the screen")
  void getByScreen_success() {
    when(showRepository.findByScreenId(1L)).thenReturn(List.of(show));
    when(showSeatRepository.findByShowIdAndStatus(1L, ShowSeatStatus.AVAILABLE))
        .thenReturn(List.of(ss1, ss2));

    List<ShowResponse> result = showService.getByScreen(1L);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAvailableSeats()).isEqualTo(2);
  }

  @Test
  @DisplayName("getUpcoming() returns shows starting after now")
  void getUpcoming_success() {
    when(showRepository.findUpcomingShows(any())).thenReturn(List.of(show));
    when(showSeatRepository.findByShowIdAndStatus(1L, ShowSeatStatus.AVAILABLE))
        .thenReturn(List.of());

    List<ShowResponse> result = showService.getUpcoming();
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getSeats() returns all show seats mapped to DTOs")
  void getSeats_success() {
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(showSeatRepository.findByShowId(1L)).thenReturn(List.of(ss1, ss2));

    List<ShowSeatResponse> result = showService.getSeats(1L);
    assertThat(result).hasSize(2);
    assertThat(result).extracting(ShowSeatResponse::getStatus).containsOnly("AVAILABLE");
  }

  @Test
  @DisplayName("BLOCKED seats are not seeded as ShowSeats")
  void create_blockedSeatsNotSeeded() {
    Seat blocked = Seat.builder().id(3L).label("B1").rowNumber(1).colNumber(0)
        .type(Seat.SeatType.BLOCKED).screenId(1L).build();
    ShowRequest req = new ShowRequest();
    req.setMovieName("Movie");
    req.setStartTime(LocalDateTime.now().plusDays(1));
    req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
    req.setBasePriceInPaise(10000);

    when(screenService.findById(1L)).thenReturn(screen);
    when(showRepository.save(any())).thenReturn(show);
    when(seatRepository.findByScreenId(1L)).thenReturn(List.of(seat1, blocked));
    when(showSeatRepository.findByShowIdAndStatus(1L, ShowSeatStatus.AVAILABLE))
        .thenReturn(List.of(ss1));

    showService.create(1L, req);

    verify(showSeatRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
  }
}
