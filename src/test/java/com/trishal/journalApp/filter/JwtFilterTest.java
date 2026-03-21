package com.trishal.journalApp.filter;

import com.trishal.journalApp.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtUtil jwtUtil;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUser_whenValidBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.extractUserName("valid-token")).thenReturn("testuser");
        when(jwtUtil.validateToken("valid-token", "testuser")).thenReturn(true);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticate_whenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticate_whenHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticate_whenTokenExtractionFails() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(request.getRequestURI()).thenReturn("/journal");
        when(jwtUtil.extractUserName("bad-token")).thenThrow(new RuntimeException("Invalid token"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticate_whenTokenIsInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtUtil.extractUserName("expired-token")).thenReturn("testuser");
        when(jwtUtil.validateToken("expired-token", "testuser")).thenReturn(false);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encoded")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotReauthenticate_whenAlreadyAuthenticated() throws Exception {
        // Pre-set authentication
        var existingAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existinguser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.extractUserName("valid-token")).thenReturn("testuser");

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Should keep existing authentication, not replace it
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existinguser");
        verify(filterChain).doFilter(request, response);
    }
}
