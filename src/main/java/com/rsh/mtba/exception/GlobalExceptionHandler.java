package com.rsh.mtba.exception;

import com.rsh.mtba.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      ResourceNotFoundException ex, HttpServletRequest req) {
    log.warn("Resource not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiError.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build());
  }

  @ExceptionHandler(SeatNotAvailableException.class)
  public ResponseEntity<ApiError> handleSeatNotAvailable(
      SeatNotAvailableException ex, HttpServletRequest req) {
    log.warn("Seat not available: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiError.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Seat Not Available")
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build());
  }

  @ExceptionHandler(BookingException.class)
  public ResponseEntity<ApiError> handleBookingException(
      BookingException ex, HttpServletRequest req) {
    log.warn("Booking error: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Booking Error")
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build());
  }

  @ExceptionHandler(PaymentException.class)
  public ResponseEntity<ApiError> handlePaymentException(
      PaymentException ex, HttpServletRequest req) {
    log.warn("Payment error: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(
            ApiError.builder()
                .status(HttpStatus.PAYMENT_REQUIRED.value())
                .error("Payment Error")
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build());
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiError> handleDuplicate(
      DuplicateResourceException ex, HttpServletRequest req) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiError.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String field = ((FieldError) error).getField();
              errors.put(field, error.getDefaultMessage());
            });
    log.warn("Validation failed: {}", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .path(req.getRequestURI())
                .validationErrors(errors)
                .build());
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest req) {
    log.warn("Bad credentials for request: {}", req.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            ApiError.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Invalid email or password")
                .path(req.getRequestURI())
                .build());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest req) {
    log.warn("Access denied for request: {}", req.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You do not have permission to access this resource")
                .path(req.getRequestURI())
                .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest req) {
    log.error("Unexpected error for request {}: {}", req.getRequestURI(), ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(req.getRequestURI())
                .build());
  }
}
