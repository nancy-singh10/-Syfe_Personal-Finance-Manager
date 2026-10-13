package com.personalfinance.service;

import com.personalfinance.dto.auth.LoginRequest;
import com.personalfinance.dto.auth.RegisterRequest;
import com.personalfinance.dto.auth.RegisterResponse;
import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.entity.User;
import com.personalfinance.exception.ConflictException;
import com.personalfinance.exception.UnauthorizedException;
import com.personalfinance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("user@example.com", "encodedPass", "John Doe", "+1234567890");
        sampleUser.setId(1L);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890");

        when(userRepository.existsByUsername("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        RegisterResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(1L, response.getUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterConflictThrows() {
        RegisterRequest req = new RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890");

        when(userRepository.existsByUsername("user@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest("user@example.com", "password123");
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        MessageResponse response = authService.login(req, httpRequest, httpResponse);

        assertEquals("Login successful", response.getMessage());
        verify(securityContextRepository).saveContext(any(), eq(httpRequest), eq(httpResponse));
    }

    @Test
    void testLoginBadCredentialsThrowsUnauthorized() {
        LoginRequest req = new LoginRequest("user@example.com", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(req, httpRequest, httpResponse));
    }

    @Test
    void testLogoutSuccess() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user@example.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(httpRequest.getSession(false)).thenReturn(httpSession);

        MessageResponse response = authService.logout(httpRequest, httpResponse);

        assertEquals("Logout successful", response.getMessage());
        verify(httpSession).invalidate();
    }
}
