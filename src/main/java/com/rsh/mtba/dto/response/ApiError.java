package com.rsh.mtba.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
  private int status;
  private String error;
  private String message;
  private String path;
  @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
  private Map<String, String> validationErrors;
}
