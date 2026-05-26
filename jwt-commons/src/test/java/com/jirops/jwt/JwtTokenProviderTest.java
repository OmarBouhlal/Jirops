package com.jirops.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                "this-is-a-very-secure-32-byte-secret-key!!".getBytes()
        );
        jwtTokenProvider = new JwtTokenProvider(secret);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateToken("user-123", List.of("ROLE_USER"), 60_000);

        assertTrue(jwtTokenProvider.isTokenValid(token));
        assertEquals("user-123", jwtTokenProvider.extractUserId(token));
        assertEquals(List.of("ROLE_USER"), jwtTokenProvider.extractRoles(token));
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        String token = jwtTokenProvider.generateToken("user-123", List.of("ROLE_USER"), 5);
        Thread.sleep(15);

        assertFalse(jwtTokenProvider.isTokenValid(token));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken("user-123", List.of("ROLE_USER"), 60_000);
        String tamperedToken = token + "a";

        assertFalse(jwtTokenProvider.isTokenValid(tamperedToken));
    }
}
