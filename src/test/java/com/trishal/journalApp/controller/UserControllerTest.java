package com.trishal.journalApp.controller;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.dto.ApiResponse;
import com.trishal.journalApp.dto.UserUpdateRequestDto;
import com.trishal.journalApp.dto.UserUpdateResponseDto;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.mapper.UserMapper;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeatherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private WeatherService weatherService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("testuser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        testUser = User.builder()
                .userId(UUID.randomUUID()).userName("testuser").password("encoded").build();
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void updateUser_shouldReturnUpdatedResponse() {
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder()
                .userName("newname").password("newpass").build();
        UserUpdateResponseDto responseDto = UserUpdateResponseDto.builder()
                .userId(testUser.getUserId()).userName("newname").build();

        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(userService.updateUser(testUser, dto)).thenReturn(testUser);
        when(userMapper.toUpdateResponse(testUser)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<UserUpdateResponseDto>> response = userController.updateUser(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getUserName()).isEqualTo("newname");
    }

    @Test
    void deleteUser_shouldReturnSuccess() {
        ResponseEntity<ApiResponse<Void>> response = userController.deleteUser();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(userService).deleteByUserName("testuser");
    }

    @Test
    void greetings_shouldIncludeWeatherInfo_whenAvailable() {
        WeatherResponse.Main main = WeatherResponse.Main.builder().feelsLike(303.15).build();
        WeatherResponse weather = WeatherResponse.builder()
                .main(main)
                .weather(List.of(WeatherResponse.Weather.builder().description("clear sky").build()))
                .build();
        when(weatherService.getWeather(28.6, 77.2)).thenReturn(weather);

        ResponseEntity<ApiResponse<String>> response = userController.greetings(28.6, 77.2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).contains("testuser").contains("clear sky");
    }

    @Test
    void greetings_shouldReturnGreetingOnly_whenWeatherIsNull() {
        when(weatherService.getWeather(28.6, 77.2)).thenReturn(null);

        ResponseEntity<ApiResponse<String>> response = userController.greetings(28.6, 77.2);

        assertThat(response.getBody().getData()).isEqualTo("Hi testuser");
    }
}