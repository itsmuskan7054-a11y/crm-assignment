package com.palmonas.crm.auth;

import com.palmonas.crm.config.AppProperties;
import com.palmonas.crm.module.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("test-secret-key-for-unit-tests-minimum-32-characters-long-enough-for-hs512");
        props.getJwt().setAccessTokenExpiry(900000);
        props.getJwt().setRefreshTokenExpiry(604800000);

        tokenProvider = new JwtTokenProvider(props);
        tokenProvider.init();
    }

    @Test
    void shouldGenerateValidAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateAccessToken(userId, "test@example.com", "ADMIN");

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
        assertEquals("ADMIN", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void shouldRejectEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void shouldGenerateUniqueRefreshTokens() {
        String token1 = tokenProvider.generateRefreshToken();
        String token2 = tokenProvider.generateRefreshToken();
        assertNotEquals(token1, token2);
    }
}
