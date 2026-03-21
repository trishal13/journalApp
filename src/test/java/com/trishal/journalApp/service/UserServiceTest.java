package com.trishal.journalApp.service;

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

        User saved = captor.getValue();
        // Password should be BCrypt encoded (starts with $2a$)
        assertThat(saved.getPassword()).startsWith("$2a$");
        assertThat(saved.getRoles()).containsExactly("USER");
    }

    @Test
    void saveNewUser_shouldThrowJournalAppException_whenRepoFails() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> userService.saveNewUser(testUser))
                .isInstanceOf(JournalAppException.class);
    }

    @Test
    void saveNewUser_shouldThrowJournalAppException_whenUserAlreadyExists() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(true);

        assertThatThrownBy(() -> userService.saveNewUser(testUser))
                .isInstanceOf(JournalAppException.class)
                .hasMessageContaining("already exists");
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

        User saved = captor.getValue();
        assertThat(saved.getPassword()).startsWith("$2a$");
        assertThat(saved.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void saveAdmin_shouldThrowJournalAppException_whenRepoFails() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> userService.saveAdmin(testUser))
                .isInstanceOf(JournalAppException.class);
    }

    @Test
    void saveAdmin_shouldThrowJournalAppException_whenUserAlreadyExists() {
        when(userRepo.existsByUserName(testUser.getUserName())).thenReturn(true);

        assertThatThrownBy(() -> userService.saveAdmin(testUser))
                .isInstanceOf(JournalAppException.class)
                .hasMessageContaining("already exists");
        verify(userRepo, never()).save(any());
    }

    // ── saveEntry (no re-encode) ─────────────────────────────────────────────

    @Test
    void saveEntry_shouldPersistWithoutReEncodingPassword() {
        String originalPassword = testUser.getPassword();
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.saveEntry(testUser);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo(originalPassword);
    }

    @Test
    void saveEntry_shouldThrowJournalAppException_whenRepoFails() {
        when(userRepo.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> userService.saveEntry(testUser))
                .isInstanceOf(JournalAppException.class);
    }

    // ── findByUserName ───────────────────────────────────────────────────────

    @Test
    void findByUserName_shouldReturnUser_whenExists() {
        when(userRepo.findByUserName("testuser")).thenReturn(testUser);

        User result = userService.findByUserName("testuser");

        assertThat(result).isEqualTo(testUser);
    }

    @Test
    void findByUserName_shouldThrowUserNotFoundException_whenNotFound() {
        when(userRepo.findByUserName("ghost")).thenReturn(null);

        assertThatThrownBy(() -> userService.findByUserName("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── getAll ───────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnAllUsers() {
        List<User> users = List.of(testUser);
        when(userRepo.findAll()).thenReturn(users);

        assertThat(userService.getAll()).hasSize(1);
    }

    // ── getUserById ──────────────────────────────────────────────────────────

    @Test
    void getUserById_shouldReturnOptionalUser() {
        UUID id = testUser.getUserId();
        when(userRepo.findById(id)).thenReturn(Optional.of(testUser));

        assertThat(userService.getUserById(id)).isPresent();
    }

    @Test
    void getUserById_shouldReturnEmpty_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepo.findById(id)).thenReturn(Optional.empty());

        assertThat(userService.getUserById(id)).isEmpty();
    }

    // ── deleteByUserName ─────────────────────────────────────────────────────

    @Test
    void deleteByUserName_shouldDelegateToRepo() {
        when(userRepo.deleteByUserName("testuser")).thenReturn(testUser);

        User deleted = userService.deleteByUserName("testuser");

        assertThat(deleted).isEqualTo(testUser);
        verify(userRepo).deleteByUserName("testuser");
    }

    @Test
    void deleteByUserName_shouldThrowJournalAppException_whenRepoFails() {
        when(userRepo.deleteByUserName("testuser")).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> userService.deleteByUserName("testuser"))
                .isInstanceOf(JournalAppException.class);
    }

    // ── deleteUserById ───────────────────────────────────────────────────────

    @Test
    void deleteUserById_shouldDelegateToRepo() {
        UUID id = testUser.getUserId();

        userService.deleteUserById(id);

        verify(userRepo).deleteById(id);
    }
}
