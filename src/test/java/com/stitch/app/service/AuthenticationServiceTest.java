package com.stitch.app.service;

import com.stitch.app.dto.AuthenticationRequest;
import com.stitch.app.dto.AuthenticationResponse;
import com.stitch.app.entity.User;
import com.stitch.app.repository.UserRepository;
import com.stitch.app.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationServiceTest {

    private AuthenticationService authenticationService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthenticationManager authenticationManager;
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(JwtService.class);
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        notificationService = Mockito.mock(NotificationService.class);

        authenticationService = new AuthenticationService(userRepository, passwordEncoder, jwtService, authenticationManager, notificationService);
    }

    @Test
    public void authenticate_shouldReturnTokenAndUser_whenCredentialsValid() {
        String email = "alice@example.com";
        String rawPassword = "password123";

        AuthenticationRequest request = new AuthenticationRequest(email, rawPassword);

        User user = User.builder()
                .id(1L)
                .email(email)
                .password("encoded")
                .fullName("Alice")
                .phoneNumber("1234567890")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        // mock behavior
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(jwtService.generateToken(ArgumentMatchers.any())).thenReturn("mocked-jwt-token");
        // authenticationManager.authenticate should not throw for valid credentials
        Authentication authMock = Mockito.mock(Authentication.class);
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);

        AuthenticationResponse resp = authenticationService.authenticate(request);

        assertNotNull(resp);
        assertEquals("mocked-jwt-token", resp.getToken());
        assertNotNull(resp.getUser());
        assertEquals(email, resp.getUser().getEmail());
    }

    @Test
    public void authenticate_shouldThrow_whenUserNotFound() {
        String email = "missing@example.com";
        AuthenticationRequest request = new AuthenticationRequest(email, "pwd");

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        // ensure authenticationManager does not throw (it may be called first), but repository will cause exception later
        Mockito.when(authenticationManager.authenticate(ArgumentMatchers.any())).thenReturn(Mockito.mock(Authentication.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authenticationService.authenticate(request));
        assertTrue(ex.getMessage().toLowerCase().contains("user not found"));
    }
}

