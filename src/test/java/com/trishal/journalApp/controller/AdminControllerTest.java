package com.trishal.journalApp.controller;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.dto.*;
import com.trishal.journalApp.entity.ConfigJournalApp;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.mapper.JournalEntryMapper;
import com.trishal.journalApp.mapper.UserMapper;
import com.trishal.journalApp.service.ConfigService;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeeklySentimentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private UserService userService;
    @Mock private AppCache appCache;
    @Mock private WeeklySentimentService weeklySentimentService;
    @Mock private ConfigService configService;
    @Mock private UserMapper userMapper;
    @Mock private JournalEntryMapper journalEntryMapper;

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
    void createAdmin_shouldDelegateToUserService() {
        User admin = User.builder().userId(UUID.randomUUID()).userName("admin").password("p").build();
        ResponseEntity<ApiResponse<User>> response = adminController.createAdmin(admin);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).saveAdmin(admin);
    }

    @Test
    void clearAppCache_shouldCallInit() {
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
        assertThat(response.getBody().getMessage()).contains("5");
    }

    @Test
    void getAllConfigs_shouldReturnConfigs() {
        ConfigJournalApp config = ConfigJournalApp.builder()
                .id(UUID.randomUUID()).key("WEATHER_API").value("https://...").build();
        when(configService.getAllConfigs()).thenReturn(List.of(config));
        var response = adminController.getAllConfigs();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getKey()).isEqualTo("WEATHER_API");
    }

    @Test
    void getAllConfigs_shouldReturnNotFound_whenEmpty() {
        when(configService.getAllConfigs()).thenReturn(Collections.emptyList());
        var response = adminController.getAllConfigs();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateConfig_shouldReturnUpdatedConfig() {
        ConfigUpdateRequestDto dto = ConfigUpdateRequestDto.builder().value("https://new-url").build();
        ConfigJournalApp updated = ConfigJournalApp.builder()
                .id(UUID.randomUUID()).key("WEATHER_API").value("https://new-url").build();
        when(configService.updateConfig("WEATHER_API", dto)).thenReturn(updated);

        var response = adminController.updateConfig("WEATHER_API", dto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getValue()).isEqualTo("https://new-url");
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() {
        User user = User.builder().userId(UUID.randomUUID()).userName("john").password("p").build();
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder().email("new@example.com").build();
        UserUpdateResponseDto responseDto = UserUpdateResponseDto.builder()
                .userId(user.getUserId()).userName("john").email("new@example.com").build();

        when(userService.findByUserName("john")).thenReturn(user);
        when(userService.updateUser(user, dto)).thenReturn(user);
        when(userMapper.toUpdateResponse(user)).thenReturn(responseDto);

        var response = adminController.updateUser("john", dto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void getUserJournals_shouldReturnEntries() {
        User user = User.builder().userId(UUID.randomUUID()).userName("john").password("p")
                .journalEntries(List.of(JournalEntry.builder().id(UUID.randomUUID()).title("T").build()))
                .build();
        when(userService.findByUserName("john")).thenReturn(user);
        JournalEntryResponseDto dto = JournalEntryResponseDto.builder().title("T").build();
        when(journalEntryMapper.toResponseList(anyList())).thenReturn(List.of(dto));

        var response = adminController.getUserJournals("john");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void getUserJournals_shouldReturnEmpty_whenNoEntries() {
        User user = User.builder().userId(UUID.randomUUID()).userName("john").password("p")
                .journalEntries(new ArrayList<>()).build();
        when(userService.findByUserName("john")).thenReturn(user);
        var response = adminController.getUserJournals("john");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEmpty();
    }
}