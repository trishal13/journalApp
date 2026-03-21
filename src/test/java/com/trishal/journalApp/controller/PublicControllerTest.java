package com.trishal.journalApp.controller;

import com.trishal.journalApp.dto.ApiResponse;
import com.trishal.journalApp.dto.UserLoginResponseDto;
import com.trishal.journalApp.dto.UserResponseDto;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.mapper.UserMapper;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.impl.UserDetailServiceImpl;
import com.trishal.journalApp.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserService userService;
    @Mock private UserDetailServiceImpl userDetailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private PublicController publicController;

    @Test
    void healthCheck_shouldReturnOk() {
        ResponseEntity<ApiResponse<String>> response = publicController.healthCheck();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo("OK");
    }

    @Test
    void signup_shouldReturnCreatedWithUserResponse() {
        var request = com.trishal.journalApp.dto.UserRegistrationRequestDto.builder()
                .userName("newuser")
                .password("password123")
                .email("new@example.com")
                .build();

        User user = User.builder()
                .userId(UUID.randomUUID())
                .userName("newuser")
                .password("password123")
                .email("new@example.com")
                .build();

        UserResponseDto responseDto = UserResponseDto.builder()
                .userId(user.getUserId())
                .userName("newuser")
                .email("new@example.com")
                .build();

        when(userMapper.toEntity(request)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<UserResponseDto>> response = publicController.signup(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getUserName()).isEqualTo("newuser");
        verify(userService).saveNewUser(user);
    }

    @Test
    void login_shouldReturnTokenOnSuccess() {
        var loginRequest = com.trishal.journalApp.dto.UserLoginRequestDto.builder()
                .userName("testuser")
                .password("password123")
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetailService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.generateToken("testuser")).thenReturn("jwt-token-123");

        User user = User.builder()
                .userId(UUID.randomUUID())
                .userName("testuser")
                .password("encoded")
                .roles(List.of("USER"))
                .build();
        when(userService.findByUserName("testuser")).thenReturn(user);

        var response = publicController.login(loginRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getBody().getData().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getData().getUserName()).isEqualTo("testuser");
        assertThat(response.getBody().getData().getRoles()).containsExactly("USER");
    }
}
