package com.trishal.journalApp.controller;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.dto.ApiResponse;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeeklySentimentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private UserService userService;
    @Mock private AppCache appCache;
    @Mock private WeeklySentimentService weeklySentimentService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getAllUsers_shouldReturnUsers_whenUsersExist() {
        User user = User.builder().userId(UUID.randomUUID()).userName("user1").password("p").build();
        when(userService.getAll()).thenReturn(List.of(user));

        ResponseEntity<ApiResponse<List<User>>> response = adminController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void getAllUsers_shouldReturnNotFound_whenNoUsers() {
        when(userService.getAll()).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<User>>> response = adminController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void getAllUsers_shouldReturnNotFound_whenNull() {
        when(userService.getAll()).thenReturn(null);

        ResponseEntity<ApiResponse<List<User>>> response = adminController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void createAdmin_shouldDelegateToUserService() {
        User admin = User.builder().userId(UUID.randomUUID()).userName("admin").password("p").build();

        ResponseEntity<ApiResponse<User>> response = adminController.createAdmin(admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(userService).saveAdmin(admin);
    }

    @Test
    void clearAppCache_shouldCallInitAndReturnSuccess() {
        ResponseEntity<ApiResponse<Void>> response = adminController.clearAppCache();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(appCache).init();
    }

    @Test
    void triggerWeeklySentimentReport_shouldReturnProcessedCount() {
        when(weeklySentimentService.runWeeklySentimentReport()).thenReturn(5);

        ResponseEntity<ApiResponse<Void>> response = adminController.triggerWeeklySentimentReport();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("5");
    }
}
