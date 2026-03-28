package com.trishal.journalApp.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Must be at least 32 chars for HS256
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "this-is-a-test-secret-key-32chars!");
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken("testuser");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUserName_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("testuser");

        String extracted = jwtUtil.extractUserName(token);

        assertThat(extracted).isEqualTo("testuser");
    }

    @Test
    void validateToken_shouldReturnTrue_forValidToken() {
        String token = jwtUtil.generateToken("testuser");

        boolean valid = jwtUtil.validateToken(token, "testuser");

        assertThat(valid).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_forDifferentUsername() {
        String token = jwtUtil.generateToken("testuser");
        boolean valid = jwtUtil.validateToken(token, "otheruser");
        assertThat(valid).isFalse();
    }

    @Test
    void extractUserName_shouldThrow_forMalformedToken() {
        assertThatThrownBy(() -> jwtUtil.extractUserName("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractUserName_shouldThrow_forTokenSignedWithDifferentKey() {
        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secretKey", "a-completely-different-secret-key!!");

        String tokenFromOther = otherJwtUtil.generateToken("testuser");

        assertThatThrownBy(() -> jwtUtil.extractUserName(tokenFromOther))
                .isInstanceOf(Exception.class);
    }

    @Test
    void generateToken_shouldProduceDifferentTokensForDifferentUsers() {
        String token1 = jwtUtil.generateToken("user1");
        String token2 = jwtUtil.generateToken("user2");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateToken_shouldProduceThreePartJwt() {
        String token = jwtUtil.generateToken("testuser");

        // JWT format: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
    }
}
