package com.trishal.journalApp.service;

import com.trishal.journalApp.dto.UserUpdateRequestDto;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.exception.UserNotFoundException;
import com.trishal.journalApp.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(UUID.randomUUID())
                .userName("testuser")
                .password("rawPassword123")
                .email("test@example.com")
                .sentimentAnalysis(true)
                .build();
    }

    // ── saveNewUser ──────────────────────────────────────────────────────────

    @Test
    void saveNewUser_shouldEncodePasswordAndSetUserRole() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.saveNewUser(testUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).startsWith("$2a$");
        assertThat(captor.getValue().getRoles()).containsExactly("USER");
    }

    @Test
    void saveNewUser_shouldThrow_whenRepoFails() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> userService.saveNewUser(testUser)).isInstanceOf(JournalAppException.class);
    }

    @Test
    void saveNewUser_shouldThrow_whenUserAlreadyExists() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(true);
        assertThatThrownBy(() -> userService.saveNewUser(testUser))
                .isInstanceOf(JournalAppException.class).hasMessageContaining("already exists");
        verify(userRepo, never()).save(any());
    }

    // ── saveAdmin ────────────────────────────────────────────────────────────

    @Test
    void saveAdmin_shouldEncodePasswordAndSetAdminRoles() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        userService.saveAdmin(testUser);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).startsWith("$2a$");
        assertThat(captor.getValue().getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void saveAdmin_shouldThrow_whenRepoFails() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> userService.saveAdmin(testUser)).isInstanceOf(JournalAppException.class);
    }

    @Test
    void saveAdmin_shouldThrow_whenUserAlreadyExists() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(true);
        assertThatThrownBy(() -> userService.saveAdmin(testUser))
                .isInstanceOf(JournalAppException.class).hasMessageContaining("already exists");
        verify(userRepo, never()).save(any());
    }

    // ── updateUser ───────────────────────────────────────────────────────────

    @Test
    void updateUser_shouldUpdateAllFields() {
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder()
                .userName("newname").password("newpass").email("new@example.com").sentimentAnalysis(false).build();
        when(userRepo.existsByUserName("newname")).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUser(testUser, dto);

        assertThat(result.getUserName()).isEqualTo("newname");
        assertThat(result.getPassword()).startsWith("$2a$");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.isSentimentAnalysis()).isFalse();
    }

    @Test
    void updateUser_shouldNotUpdateBlankFields() {
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder().userName("").password("").email("").build();
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(testUser, dto);

        assertThat(testUser.getUserName()).isEqualTo("testuser");
        assertThat(testUser.getPassword()).isEqualTo("rawPassword123");
    }

    @Test
    void updateUser_shouldThrow_whenNewUsernameAlreadyExists() {
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder().userName("taken").build();
        when(userRepo.existsByUserName("taken")).thenReturn(true);
        assertThatThrownBy(() -> userService.updateUser(testUser, dto))
                .isInstanceOf(JournalAppException.class).hasMessageContaining("already exists");
    }

    @Test
    void updateUser_shouldNotCheckDuplicate_whenUsernameSame() {
        UserUpdateRequestDto dto = UserUpdateRequestDto.builder().userName("testuser").build();
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        userService.updateUser(testUser, dto);
        verify(userRepo, never()).existsByUserName(anyString());
    }

    // ── saveEntry (no re-encode) ─────────────────────────────────────────────

    @Test
    void saveEntry_shouldPersistWithoutReEncodingPassword() {
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        userService.saveEntry(testUser);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("rawPassword123");
    }

    @Test
    void saveEntry_shouldThrow_whenRepoFails() {
        when(userRepo.save(any(User.class))).thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> userService.saveEntry(testUser)).isInstanceOf(JournalAppException.class);
    }

    // ── findByUserName ───────────────────────────────────────────────────────

    @Test
    void findByUserName_shouldReturnUser() {
        when(userRepo.findByUserName("testuser")).thenReturn(testUser);
        assertThat(userService.findByUserName("testuser")).isEqualTo(testUser);
    }

    @Test
    void findByUserName_shouldThrow_whenNotFound() {
        when(userRepo.findByUserName("ghost")).thenReturn(null);
        assertThatThrownBy(() -> userService.findByUserName("ghost")).isInstanceOf(UserNotFoundException.class);
    }

    // ── getAll / getUserById ─────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnAllUsers() {
        when(userRepo.findAll()).thenReturn(List.of(testUser));
        assertThat(userService.getAll()).hasSize(1);
    }

    @Test
    void getUserById_shouldReturnOptional() {
        when(userRepo.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        assertThat(userService.getUserById(testUser.getUserId())).isPresent();
    }

    // ── deleteByUserName ─────────────────────────────────────────────────────

    @Test
    void deleteByUserName_shouldDelegateToRepo() {
        userService.deleteByUserName("testuser");
        verify(userRepo).deleteByUserName("testuser");
    }

    @Test
    void deleteByUserName_shouldThrow_whenRepoFails() {
        doThrow(new RuntimeException("DB error")).when(userRepo).deleteByUserName("testuser");
        assertThatThrownBy(() -> userService.deleteByUserName("testuser")).isInstanceOf(JournalAppException.class);
    }

    // ── deleteUserById ───────────────────────────────────────────────────────

    @Test
    void deleteUserById_shouldDelegateToRepo() {
        userService.deleteUserById(testUser.getUserId());
        verify(userRepo).deleteById(testUser.getUserId());
    }
}