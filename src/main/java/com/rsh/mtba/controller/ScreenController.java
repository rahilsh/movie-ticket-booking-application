package com.rsh.mtba.controller;

import com.rsh.mtba.dto.request.ScreenRequest;
import com.rsh.mtba.dto.response.ScreenResponse;
import com.rsh.mtba.service.ScreenService;
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
@Tag(name = "Screens", description = "Screen management endpoints")
public class ScreenController {

  private final ScreenService screenService;

  @PostMapping("/api/theatres/{theatreId}/screens")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Add a screen to a theatre (Admin only)")
  public ResponseEntity<ScreenResponse> create(
      @PathVariable Long theatreId, @Valid @RequestBody ScreenRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(screenService.create(theatreId, request));
  }

  @GetMapping("/api/theatres/{theatreId}/screens")
  @Operation(summary = "Get all screens for a theatre")
  public ResponseEntity<List<ScreenResponse>> getByTheatre(@PathVariable Long theatreId) {
    return ResponseEntity.ok(screenService.getByTheatre(theatreId));
  }

  @GetMapping("/api/screens/{id}")
  @Operation(summary = "Get screen by ID")
  public ResponseEntity<ScreenResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(screenService.getById(id));
  }
}
