package com.stitch.app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private String secret;

    @BeforeEach
    public void setUp() throws Exception {
        jwtService = new JwtService();
        userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn("testuser");

        // generate a 256-bit (32-byte) key and Base64-encode it
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        secret = Base64.getEncoder().encodeToString(keyBytes);

        setPrivateField(jwtService, "secretKey", secret);
    }

    @Test
    public void shouldGenerateAndValidateToken() throws Exception {
        setPrivateField(jwtService, "jwtExpiration", 10_000L); // 10 seconds

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertEquals("testuser", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    public void shouldDetectExpiredToken() throws Exception {
        setPrivateField(jwtService, "jwtExpiration", -1_000L); // already expired

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    // helper to set private fields
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

