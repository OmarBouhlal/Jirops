// jwt-commons/src/test/java/com/jiraclone/commons/JwtTokenProviderTest.java
package com.jiraclone.commons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("generateAndValidateAccessToken")
    void generateAndValidateAccessToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 120_000);

        String token = provider.generateAccessToken("user-1", List.of("ROLE_USER"));

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("expiredTokenReturnsFalse")
    void expiredTokenReturnsFalse() throws InterruptedException {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 1, 120_000);

        String token = provider.generateAccessToken("user-1", List.of("ROLE_USER"));
        Thread.sleep(5L);

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("tamperedTokenReturnsFalse")
    void tamperedTokenReturnsFalse() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 120_000);
        String token = provider.generateAccessToken("user-1", List.of("ROLE_USER"));
        int middleIndex = token.length() / 2;
        char replacement = token.charAt(middleIndex) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, middleIndex) + replacement + token.substring(middleIndex + 1);

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("getUserIdFromToken")
    void getUserIdFromToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 120_000);

        String token = provider.generateAccessToken("user-42", List.of("ROLE_USER"));

        assertThat(provider.getUserIdFromToken(token)).isEqualTo("user-42");
    }

    @Test
    @DisplayName("getRolesFromToken")
    void getRolesFromToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 120_000);

        String token = provider.generateAccessToken("user-42", List.of("ROLE_USER", "ROLE_ADMIN"));

        assertThat(provider.getRolesFromToken(token)).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("refreshTokenIsNotAccessToken")
    void refreshTokenIsNotAccessToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 120_000);

        String refreshToken = provider.generateRefreshToken("user-42");

        assertThat(provider.isAccessToken(refreshToken)).isFalse();
    }
}
