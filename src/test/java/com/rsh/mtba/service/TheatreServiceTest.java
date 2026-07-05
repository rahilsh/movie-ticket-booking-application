package com.rsh.mtba.service;

import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.entity.Theatre;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.TheatreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TheatreServiceTest {

  @Mock private TheatreRepository theatreRepository;
  @InjectMocks private TheatreService theatreService;

  @Test
  @DisplayName("create() saves a theatre and returns response DTO")
  void create_success() {
    TheatreRequest request = new TheatreRequest();
    request.setName("PVR Cinemas");
    request.setAddress("MG Road");
    request.setCity("Bangalore");

    Theatre saved = Theatre.builder().id(1L).name("PVR Cinemas").address("MG Road").city("Bangalore").build();
    when(theatreRepository.save(any())).thenReturn(saved);

    TheatreResponse response = theatreService.create(request);
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getName()).isEqualTo("PVR Cinemas");
    assertThat(response.getCity()).isEqualTo("Bangalore");
  }

  @Test
  @DisplayName("getById() throws ResourceNotFoundException when theatre does not exist")
  void getById_notFound_throws() {
    when(theatreRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> theatreService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getByCity() returns only theatres in the given city")
  void getByCity_returnsFiltered() {
    Theatre t1 = Theatre.builder().id(1L).name("PVR").address("MG Road").city("Bangalore").build();
    Theatre t2 = Theatre.builder().id(2L).name("INOX").address("Forum").city("Bangalore").build();
    when(theatreRepository.findByCity("Bangalore")).thenReturn(List.of(t1, t2));

    List<TheatreResponse> result = theatreService.getByCity("Bangalore");
    assertThat(result).hasSize(2);
    assertThat(result).extracting(TheatreResponse::getCity).containsOnly("Bangalore");
  }

  @Test
  @DisplayName("delete() calls repository delete once")
  void delete_success() {
    Theatre theatre = Theatre.builder().id(1L).name("PVR").address("MG Road").city("Bangalore").build();
    when(theatreRepository.findById(1L)).thenReturn(Optional.of(theatre));

    theatreService.delete(1L);

    verify(theatreRepository, times(1)).delete(1L);
  }
}
