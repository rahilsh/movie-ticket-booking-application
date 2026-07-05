package com.rsh.mtba.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.entity.Theatre;
import com.rsh.mtba.exception.ResourceNotFoundException;
import com.rsh.mtba.repository.TheatreRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TheatreServiceExtendedTest {

  @Mock private TheatreRepository theatreRepository;
  @InjectMocks private TheatreService theatreService;

  @Test
  @DisplayName("update() persists updated fields")
  void update_success() {
    Theatre existing = Theatre.builder().id(1L).name("Old").address("Old").city("OldCity").build();
    when(theatreRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(theatreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TheatreRequest req = new TheatreRequest();
    req.setName("New");
    req.setAddress("New Addr");
    req.setCity("NewCity");

    TheatreResponse resp = theatreService.update(1L, req);
    assertThat(resp.getName()).isEqualTo("New");
    assertThat(resp.getCity()).isEqualTo("NewCity");
  }

  @Test
  @DisplayName("update() throws ResourceNotFoundException for unknown id")
  void update_notFound() {
    when(theatreRepository.findById(99L)).thenReturn(Optional.empty());
    TheatreRequest req = new TheatreRequest();
    req.setName("X");
    req.setAddress("Y");
    req.setCity("Z");
    assertThatThrownBy(() -> theatreService.update(99L, req))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getAll() returns all theatres")
  void getAll_success() {
    Theatre t1 = Theatre.builder().id(1L).name("A").address("A").city("C1").build();
    Theatre t2 = Theatre.builder().id(2L).name("B").address("B").city("C2").build();
    when(theatreRepository.findAll()).thenReturn(List.of(t1, t2));

    List<TheatreResponse> result = theatreService.getAll();
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("delete() throws ResourceNotFoundException for unknown id")
  void delete_notFound() {
    when(theatreRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> theatreService.delete(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
