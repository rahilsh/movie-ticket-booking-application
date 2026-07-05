package com.rsh.mtba.controller;

import com.rsh.mtba.dto.request.TheatreRequest;
import com.rsh.mtba.dto.response.TheatreResponse;
import com.rsh.mtba.service.TheatreService;
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
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
@Tag(name = "Theatres", description = "Theatre management endpoints")
public class TheatreController {

  private final TheatreService theatreService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Create a new theatre (Admin only)")
  public ResponseEntity<TheatreResponse> create(@Valid @RequestBody TheatreRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(theatreService.create(request));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get theatre by ID")
  public ResponseEntity<TheatreResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(theatreService.getById(id));
  }

  @GetMapping
  @Operation(summary = "Get all theatres, optionally filtered by city")
  public ResponseEntity<List<TheatreResponse>> getAll(@RequestParam(required = false) String city) {
    if (city != null && !city.isBlank()) {
      return ResponseEntity.ok(theatreService.getByCity(city));
    }
    return ResponseEntity.ok(theatreService.getAll());
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update a theatre (Admin only)")
  public ResponseEntity<TheatreResponse> update(
      @PathVariable Long id, @Valid @RequestBody TheatreRequest request) {
    return ResponseEntity.ok(theatreService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete a theatre (Admin only)")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    theatreService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
