package com.rsh.mtba.controller;

import com.rsh.mtba.dto.request.PaymentRequest;
import com.rsh.mtba.dto.response.PaymentResponse;
import com.rsh.mtba.entity.User;
import com.rsh.mtba.repository.UserRepository;
import com.rsh.mtba.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment initiation and confirmation endpoints")
public class PaymentController {

  private final PaymentService paymentService;
  private final UserRepository userRepository;

  @PostMapping
  @Operation(summary = "Initiate a payment for a booking")
  public ResponseEntity<PaymentResponse> initiatePayment(
      @Valid @RequestBody PaymentRequest request, Authentication authentication) {
    Long userId = resolveUserId(authentication);
    return ResponseEntity.ok(paymentService.initiatePayment(userId, request));
  }

  @PostMapping("/confirm/{transactionId}")
  @Operation(summary = "Confirm a payment (simulates gateway callback)")
  public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable String transactionId) {
    return ResponseEntity.ok(paymentService.confirmPayment(transactionId));
  }

  @PostMapping("/fail/{transactionId}")
  @Operation(summary = "Fail a payment (simulates gateway failure callback)")
  public ResponseEntity<PaymentResponse> failPayment(
      @PathVariable String transactionId,
      @RequestParam(defaultValue = "Payment declined") String reason) {
    return ResponseEntity.ok(paymentService.failPayment(transactionId, reason));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get payment by ID")
  public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(paymentService.getById(id));
  }

  @GetMapping("/booking/{bookingId}")
  @Operation(summary = "Get payment for a booking")
  public ResponseEntity<PaymentResponse> getByBooking(@PathVariable Long bookingId) {
    return ResponseEntity.ok(paymentService.getByBookingId(bookingId));
  }

  private Long resolveUserId(Authentication authentication) {
    String email = authentication.getName();
    return userRepository
        .findByEmail(email)
        .map(User::getId)
        .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
  }
}
