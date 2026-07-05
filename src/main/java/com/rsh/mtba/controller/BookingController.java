package com.rsh.mtba.controller;

import com.rsh.mtba.dto.request.BookingRequest;
import com.rsh.mtba.dto.response.BookingResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Ticket booking and cancellation endpoints")
public class BookingController {

  private final BookingService bookingService;
  private final UserRepository userRepository;

  @PostMapping
  @Operation(summary = "Create a new booking")
  public ResponseEntity<BookingResponse> book(
      @Valid @RequestBody BookingRequest request, Authentication authentication) {
    Long userId = resolveUserId(authentication);
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.book(userId, request));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get booking by ID")
  public ResponseEntity<BookingResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.getById(id));
  }

  @GetMapping("/my")
  @Operation(summary = "Get all bookings for the authenticated user")
  public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
    Long userId = resolveUserId(authentication);
    return ResponseEntity.ok(bookingService.getByUser(userId));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Cancel a booking")
  public ResponseEntity<BookingResponse> cancel(
      @PathVariable Long id, Authentication authentication) {
    Long userId = resolveUserId(authentication);
    return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
  }

  private Long resolveUserId(Authentication authentication) {
    String email = authentication.getName();
    return userRepository
        .findByEmail(email)
        .map(User::getId)
        .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
  }
}
