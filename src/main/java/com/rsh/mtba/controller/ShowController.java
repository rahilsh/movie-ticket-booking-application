package com.rsh.mtba.controller;

import com.rsh.mtba.dto.request.ShowRequest;
import com.rsh.mtba.dto.response.ShowResponse;
import com.rsh.mtba.dto.response.ShowSeatResponse;
import com.rsh.mtba.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Shows", description = "Show scheduling and seat availability endpoints")
public class ShowController {

  private final ShowService showService;

  @PostMapping("/api/screens/{screenId}/shows")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Schedule a show on a screen (Admin only)")
  public ResponseEntity<ShowResponse> create(
      @PathVariable Long screenId, @Valid @RequestBody ShowRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(showService.create(screenId, request));
  }

  @GetMapping("/api/shows/{id}")
  @Operation(summary = "Get show by ID")
  public ResponseEntity<ShowResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(showService.getById(id));
  }

  @GetMapping("/api/screens/{screenId}/shows")
  @Operation(summary = "Get all shows for a screen")
  public ResponseEntity<List<ShowResponse>> getByScreen(@PathVariable Long screenId) {
    return ResponseEntity.ok(showService.getByScreen(screenId));
  }

  @GetMapping("/api/shows")
  @Operation(summary = "Get upcoming shows")
  public ResponseEntity<List<ShowResponse>> getUpcoming() {
    return ResponseEntity.ok(showService.getUpcoming());
  }

  @GetMapping("/api/shows/{id}/seats")
  @Operation(summary = "Get seat layout and availability for a show")
  public ResponseEntity<List<ShowSeatResponse>> getSeats(@PathVariable Long id) {
    return ResponseEntity.ok(showService.getSeats(id));
  }
}
