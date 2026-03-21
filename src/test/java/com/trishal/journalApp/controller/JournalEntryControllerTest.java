package com.trishal.journalApp.controller;

import com.trishal.journalApp.dto.*;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.exception.JournalEntryAccessDeniedException;
import com.trishal.journalApp.exception.JournalEntryNotFoundException;
import com.trishal.journalApp.mapper.JournalEntryMapper;
import com.trishal.journalApp.service.JournalEntryService;
import com.trishal.journalApp.service.UserService;
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

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryControllerTest {

    @Mock private JournalEntryService journalEntryService;
    @Mock private UserService userService;
    @Mock private JournalEntryMapper journalEntryMapper;

    @InjectMocks
    private JournalEntryController controller;

    private User testUser;
    private JournalEntry testEntry;
    private UUID entryId;

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("testuser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        entryId = UUID.randomUUID();
        testEntry = JournalEntry.builder()
                .id(entryId).title("Test Entry").content("Test Content")
                .sentiment(Sentiment.HAPPY).build();

        testUser = User.builder()
                .userId(UUID.randomUUID()).userName("testuser").password("encoded")
                .journalEntries(new ArrayList<>(List.of(testEntry))).build();
        testEntry.setUser(testUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── GET all ───────────────────────────────────────────────────────────────

    @Test
    void getAllJournalEntries_shouldReturnEntries() {
        when(userService.findByUserName("testuser")).thenReturn(testUser);
        JournalEntryResponseDto dto = JournalEntryResponseDto.builder()
                .id(entryId).title("Test Entry").build();
        when(journalEntryMapper.toResponseList(anyList())).thenReturn(List.of(dto));

        ResponseEntity<ApiResponse<List<JournalEntryResponseDto>>> response =
                controller.getAllJournalEntriesOfUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void getAllJournalEntries_shouldReturnEmptyList_whenEmpty() {
        testUser.setJournalEntries(new ArrayList<>());
        when(userService.findByUserName("testuser")).thenReturn(testUser);

        ResponseEntity<ApiResponse<List<JournalEntryResponseDto>>> response =
                controller.getAllJournalEntriesOfUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    void getAllJournalEntries_shouldReturnEmptyList_whenNull() {
        testUser.setJournalEntries(null);
        when(userService.findByUserName("testuser")).thenReturn(testUser);

        ResponseEntity<ApiResponse<List<JournalEntryResponseDto>>> response =
                controller.getAllJournalEntriesOfUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEmpty();
    }

    // ── POST create ───────────────────────────────────────────────────────────

    @Test
    void createEntry_shouldReturnCreated() {
        JournalEntryCreateRequestDto createDto = JournalEntryCreateRequestDto.builder()
                .title("New Entry").content("Content").build();
        JournalEntryResponseDto responseDto = JournalEntryResponseDto.builder()
                .id(UUID.randomUUID()).title("New Entry").build();

        when(journalEntryMapper.toEntity(createDto)).thenReturn(testEntry);
        when(journalEntryMapper.toResponse(testEntry)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<JournalEntryResponseDto>> response = controller.createEntry(createDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getTitle()).isEqualTo("New Entry");
        verify(journalEntryService).saveEntry(testEntry, "testuser");
    }

    // ── GET by id ─────────────────────────────────────────────────────────────

    @Test
    void getJournalEntryById_shouldReturnEntry_whenOwned() {
        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(journalEntryService.getJournalEntryById(entryId)).thenReturn(Optional.of(testEntry));
        JournalEntryResponseDto dto = JournalEntryResponseDto.builder().id(entryId).build();
        when(journalEntryMapper.toResponse(testEntry)).thenReturn(dto);

        ResponseEntity<ApiResponse<JournalEntryResponseDto>> response =
                controller.getJournalEntryById(entryId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo(entryId);
    }

    @Test
    void getJournalEntryById_shouldThrow_whenNotOwned() {
        UUID otherId = UUID.randomUUID();
        when(userService.findByUserName("testuser")).thenReturn(testUser);

        assertThatThrownBy(() -> controller.getJournalEntryById(otherId))
                .isInstanceOf(JournalEntryAccessDeniedException.class);
    }

    @Test
    void getJournalEntryById_shouldThrow_whenEntryNotFound() {
        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(journalEntryService.getJournalEntryById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getJournalEntryById(entryId))
                .isInstanceOf(JournalEntryNotFoundException.class);
    }

    // ── DELETE by id ──────────────────────────────────────────────────────────

    @Test
    void deleteJournalEntryById_shouldReturnSuccess() {
        when(userService.findByUserName("testuser")).thenReturn(testUser);

        ResponseEntity<ApiResponse<Void>> response = controller.deleteJournalEntryById(entryId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(journalEntryService).deleteJournalEntryById(entryId, "testuser");
    }

    @Test
    void deleteJournalEntryById_shouldThrow_whenNotOwned() {
        UUID otherId = UUID.randomUUID();
        when(userService.findByUserName("testuser")).thenReturn(testUser);

        assertThatThrownBy(() -> controller.deleteJournalEntryById(otherId))
                .isInstanceOf(JournalEntryAccessDeniedException.class);
    }

    // ── PUT update ────────────────────────────────────────────────────────────

    @Test
    void updateJournalEntryById_shouldUpdateAndReturn() {
        JournalEntryUpdateRequestDto updateDto = JournalEntryUpdateRequestDto.builder()
                .title("Updated Title").content("Updated Content").build();
        JournalEntryResponseDto responseDto = JournalEntryResponseDto.builder()
                .id(entryId).title("Updated Title").build();

        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(journalEntryService.getJournalEntryById(entryId)).thenReturn(Optional.of(testEntry));
        when(journalEntryMapper.toResponse(testEntry)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<JournalEntryResponseDto>> response =
                controller.updateJournalEntryById(entryId, updateDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(testEntry.getTitle()).isEqualTo("Updated Title");
        assertThat(testEntry.getContent()).isEqualTo("Updated Content");
        verify(journalEntryService).saveEntry(testEntry);
    }

    @Test
    void updateJournalEntryById_shouldNotUpdateBlankFields() {
        JournalEntryUpdateRequestDto updateDto = JournalEntryUpdateRequestDto.builder()
                .title("  ").content("").build();

        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(journalEntryService.getJournalEntryById(entryId)).thenReturn(Optional.of(testEntry));
        when(journalEntryMapper.toResponse(testEntry)).thenReturn(
                JournalEntryResponseDto.builder().id(entryId).build());

        controller.updateJournalEntryById(entryId, updateDto);

        assertThat(testEntry.getTitle()).isEqualTo("Test Entry");
        assertThat(testEntry.getContent()).isEqualTo("Test Content");
    }
}
