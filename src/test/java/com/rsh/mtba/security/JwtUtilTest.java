package com.rsh.mtba.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil("test-secret-key-that-is-long-enough-for-hmac-sha256-testing", 3600000L);
  }

  @Test
  @DisplayName("generateToken() produces a non-blank token")
  void generateToken_notBlank() {
    String token = jwtUtil.generateToken("alice@test.com");
    assertThat(token).isNotBlank();
  }

  @Test
  @DisplayName("extractEmail() returns the email embedded in the token")
  void extractEmail_success() {
    String token = jwtUtil.generateToken("alice@test.com");
    assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@test.com");
  }

  @Test
  @DisplayName("tokens do not contain authorization claims")
  void generateToken_hasNoRoleClaim() {
    String token = jwtUtil.generateToken("admin@test.com");
    assertThat(token).doesNotContain("ROLE_ADMIN");
  }

  @Test
  @DisplayName("isValid() returns true for a freshly generated token")
  void isValid_validToken_returnsTrue() {
    String token = jwtUtil.generateToken("user@test.com");
    assertThat(jwtUtil.isValid(token)).isTrue();
  }

  @Test
  @DisplayName("isValid() returns false for a garbage string")
  void isValid_garbage_returnsFalse() {
    assertThat(jwtUtil.isValid("not.a.token")).isFalse();
  }

  @Test
  @DisplayName("isValid() returns false for an empty string")
  void isValid_empty_returnsFalse() {
    assertThat(jwtUtil.isValid("")).isFalse();
  }

  @Test
  @DisplayName("isValid() returns false for a token signed with a different key")
  void isValid_wrongKey_returnsFalse() {
    JwtUtil otherUtil =
        new JwtUtil("completely-different-secret-key-that-is-also-long-enough!!", 3600000L);
    String alienToken = otherUtil.generateToken("evil@test.com");
    assertThat(jwtUtil.isValid(alienToken)).isFalse();
  }

  @Test
  @DisplayName("isValid() returns false for an expired token")
  void isValid_expired_returnsFalse() {
    JwtUtil expiredUtil =
        new JwtUtil(
            "test-secret-key-that-is-long-enough-for-hmac-sha256-testing", -1000L // already expired
            );
    String token = expiredUtil.generateToken("user@test.com");
    assertThat(jwtUtil.isValid(token)).isFalse();
  }
}
