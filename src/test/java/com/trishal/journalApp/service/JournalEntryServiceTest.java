package com.trishal.journalApp.service;

import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.exception.JournalEntryNotFoundException;
import com.trishal.journalApp.exception.UserNotFoundException;
import com.trishal.journalApp.repository.JournalEntryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock
    private JournalEntryRepo journalEntryRepo;

    @Mock
    private UserService userService;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private JournalEntryService journalEntryService;

    private User testUser;
    private JournalEntry testEntry;

    @BeforeEach
    void setUp() {
        testEntry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .title("Good day")
                .content("Had a wonderful day at the park")
                .sentiment(Sentiment.HAPPY)
                .build();

        testUser = User.builder()
                .userId(UUID.randomUUID())
                .userName("testuser")
                .password("encoded")
                .journalEntries(new ArrayList<>(List.of(testEntry)))
                .build();

        testEntry.setUser(testUser);
    }

    // ── saveEntry (new entry) ────────────────────────────────────────────────

    @Test
    void saveEntry_shouldSetSentimentAndUser_thenPersist() {
        testUser.setSentimentAnalysis(true);
        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(geminiService.analyseSentiment(anyString())).thenReturn(Sentiment.HAPPY);
        when(journalEntryRepo.save(any(JournalEntry.class))).thenReturn(testEntry);

        JournalEntry newEntry = JournalEntry.builder()
                .title("New entry")
                .content("Some content")
                .build();

        journalEntryService.saveEntry(newEntry, "testuser");

        assertThat(newEntry.getSentiment()).isEqualTo(Sentiment.HAPPY);
        assertThat(newEntry.getUser()).isEqualTo(testUser);
        verify(journalEntryRepo).save(newEntry);
    }

    @Test
    void saveEntry_shouldThrowUserNotFoundException_whenUserMissing() {
        when(userService.findByUserName("ghost")).thenThrow(new UserNotFoundException("ghost"));

        JournalEntry entry = JournalEntry.builder().title("Test").build();

        assertThatThrownBy(() -> journalEntryService.saveEntry(entry, "ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void saveEntry_shouldFallbackToExistingSentiment_whenGeminiFails() {
        testUser.setSentimentAnalysis(true);
        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(geminiService.analyseSentiment(anyString()))
                .thenThrow(new JournalAppException(
                        com.trishal.journalApp.exception.ErrorCode.GEMINI_SERVICE_UNAVAILABLE));
        when(journalEntryRepo.save(any(JournalEntry.class))).thenReturn(testEntry);

        JournalEntry entry = JournalEntry.builder()
                .title("Test")
                .content("Content")
                .build(); // sentiment is null → triggers Gemini

        journalEntryService.saveEntry(entry, "testuser");

        // analyseSentimentSafely catches exception and returns existing (null)
        assertThat(entry.getSentiment()).isNull();
        verify(journalEntryRepo).save(entry);
    }

    @Test
    void saveEntry_shouldThrowJournalAppException_whenRepoFails() {
        testUser.setSentimentAnalysis(true);
        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(geminiService.analyseSentiment(anyString())).thenReturn(Sentiment.HAPPY);
        when(journalEntryRepo.save(any(JournalEntry.class))).thenThrow(new RuntimeException("DB error"));

        JournalEntry entry = JournalEntry.builder().title("Test").content("Content").build();

        assertThatThrownBy(() -> journalEntryService.saveEntry(entry, "testuser"))
                .isInstanceOf(JournalAppException.class);
    }

    // ── saveEntry (update) ───────────────────────────────────────────────────

    @Test
    void saveEntryUpdate_shouldReAnalyseSentiment() {
        testUser.setSentimentAnalysis(true);
        when(geminiService.analyseSentiment(anyString())).thenReturn(Sentiment.ANXIOUS);
        when(journalEntryRepo.save(any(JournalEntry.class))).thenReturn(testEntry);

        journalEntryService.saveEntry(testEntry, testUser);

        assertThat(testEntry.getSentiment()).isEqualTo(Sentiment.ANXIOUS);
        verify(journalEntryRepo).save(testEntry);
    }

    // ── getJournalEntryById ──────────────────────────────────────────────────

    @Test
    void getJournalEntryById_shouldReturnEntry_whenExists() {
        UUID id = testEntry.getId();
        when(journalEntryRepo.findById(id)).thenReturn(Optional.of(testEntry));

        Optional<JournalEntry> result = journalEntryService.getJournalEntryById(id);

        assertThat(result).isPresent().contains(testEntry);
    }

    @Test
    void getJournalEntryById_shouldReturnEmpty_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(journalEntryRepo.findById(id)).thenReturn(Optional.empty());

        assertThat(journalEntryService.getJournalEntryById(id)).isEmpty();
    }

    // ── getAll ───────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnAllEntries() {
        when(journalEntryRepo.findAll()).thenReturn(List.of(testEntry));

        assertThat(journalEntryService.getAll()).hasSize(1);
    }

    // ── deleteJournalEntryById ───────────────────────────────────────────────

    @Test
    void deleteJournalEntryById_shouldRemoveEntryFromUserAndRepo() {
        UUID entryId = testEntry.getId();
        when(userService.findByUserName("testuser")).thenReturn(testUser);

        boolean result = journalEntryService.deleteJournalEntryById(entryId, "testuser");

        assertThat(result).isTrue();
        assertThat(testUser.getJournalEntries()).doesNotContain(testEntry);
        verify(journalEntryRepo).deleteById(entryId);
        verify(userService).saveEntry(testUser);
    }

    @Test
    void deleteJournalEntryById_shouldThrow_whenEntryNotOwnedByUser() {
        UUID randomId = UUID.randomUUID();
        when(userService.findByUserName("testuser")).thenReturn(testUser);

        assertThatThrownBy(() -> journalEntryService.deleteJournalEntryById(randomId, "testuser"))
                .isInstanceOf(JournalEntryNotFoundException.class);
    }

    @Test
    void deleteJournalEntryById_shouldThrow_whenUserNotFound() {
        when(userService.findByUserName("ghost")).thenThrow(new UserNotFoundException("ghost"));

        assertThatThrownBy(() -> journalEntryService.deleteJournalEntryById(UUID.randomUUID(), "ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── sentiment analysis with blank text ───────────────────────────────────

    @Test
    void saveEntry_shouldKeepExistingSentiment_whenTextIsEffectivelyBlank() {
        testUser.setSentimentAnalysis(true);
        when(userService.findByUserName("testuser")).thenReturn(testUser);
        when(journalEntryRepo.save(any(JournalEntry.class))).thenReturn(testEntry);

        // Entry has sentiment already set → Gemini is NOT called (condition: isEmpty(sentiment))
        JournalEntry entry = JournalEntry.builder()
                .title("x")
                .content("")
                .sentiment(Sentiment.ANGRY)
                .build();

        journalEntryService.saveEntry(entry, "testuser");

        assertThat(entry.getSentiment()).isEqualTo(Sentiment.ANGRY);
        verify(geminiService, never()).analyseSentiment(anyString());
    }
}
