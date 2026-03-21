package com.trishal.journalApp.controller;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.dto.ApiResponse;
import com.trishal.journalApp.entity.User;
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

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("testuser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUser_shouldUpdateUsernameAndPassword() {
        User oldUser = User.builder()
                .userId(UUID.randomUUID()).userName("testuser").password("oldEncoded").build();
        User newUser = User.builder().userName("newname").password("newpassword").build();
        when(userService.findByUserName("testuser")).thenReturn(oldUser);

        ResponseEntity<ApiResponse<User>> response = userController.updateUser(newUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(oldUser.getUserName()).isEqualTo("newname");
        assertThat(oldUser.getPassword()).isEqualTo("newpassword");
        verify(userService).saveNewUser(oldUser);
    }

    @Test
    void updateUser_shouldNotUpdateEmptyFields() {
        User oldUser = User.builder()
                .userId(UUID.randomUUID()).userName("testuser").password("oldEncoded").build();
        User newUser = User.builder().userName("").password("").build();
        when(userService.findByUserName("testuser")).thenReturn(oldUser);

        userController.updateUser(newUser);

        assertThat(oldUser.getUserName()).isEqualTo("testuser");
        assertThat(oldUser.getPassword()).isEqualTo("oldEncoded");
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
        WeatherResponse weather = WeatherResponse.builder()
                .current(WeatherResponse.Current.builder().feelslike(30).build())
                .build();
        when(weatherService.getWeather("Mumbai")).thenReturn(weather);

        ResponseEntity<ApiResponse<String>> response = userController.greetings("Mumbai");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).contains("testuser").contains("30");
    }

    @Test
    void greetings_shouldReturnGreetingOnly_whenWeatherIsNull() {
        when(weatherService.getWeather("Mumbai")).thenReturn(null);

        ResponseEntity<ApiResponse<String>> response = userController.greetings("Mumbai");

        assertThat(response.getBody().getData()).isEqualTo("Hi testuser");
    }
}
