package com.trishal.journalApp.service;

import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.kafka.producer.SentimentKafkaProducer;
import com.trishal.journalApp.model.SentimentData;
import com.trishal.journalApp.repository.impl.UserRepoImpl;
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
class WeeklySentimentServiceTest {

    @Mock
    private UserRepoImpl userRepoImpl;

    @Mock
    private EmailService emailService;

    @Mock
    private SentimentKafkaProducer sentimentKafkaProducer;

    @InjectMocks
    private WeeklySentimentService weeklySentimentService;

    private User testUser;

    @BeforeEach
    void setUp() {
        JournalEntry entry1 = JournalEntry.builder()
                .id(UUID.randomUUID())
                .title("Happy day")
                .content("Great stuff")
                .sentiment(Sentiment.HAPPY)
                .date(new Date()) // today — within 7 days
                .build();

        JournalEntry entry2 = JournalEntry.builder()
                .id(UUID.randomUUID())
                .title("Another happy day")
                .content("More great stuff")
                .sentiment(Sentiment.HAPPY)
                .date(new Date())
                .build();

        testUser = User.builder()
                .userId(UUID.randomUUID())
                .userName("testuser")
                .password("encoded")
                .email("test@example.com")
                .sentimentAnalysis(true)
                .journalEntries(new ArrayList<>(List.of(entry1, entry2)))
                .build();
    }

    @Test
    void runWeeklySentimentReport_shouldProcessEligibleUsers() {
        when(userRepoImpl.getUsersForSentimentAnalysis()).thenReturn(List.of(testUser));

        int processed = weeklySentimentService.runWeeklySentimentReport();

        assertThat(processed).isEqualTo(1);
        verify(sentimentKafkaProducer).sendWeeklySentiment(any(SentimentData.class));
    }

    @Test
    void runWeeklySentimentReport_shouldReturnZero_whenNoEligibleUsers() {
        when(userRepoImpl.getUsersForSentimentAnalysis()).thenReturn(Collections.emptyList());

        int processed = weeklySentimentService.runWeeklySentimentReport();

        assertThat(processed).isZero();
        verify(sentimentKafkaProducer, never()).sendWeeklySentiment(any());
    }

    @Test
    void runWeeklySentimentReport_shouldSkipUsersWithNoRecentEntries() {
        testUser.setJournalEntries(new ArrayList<>()); // no entries
        when(userRepoImpl.getUsersForSentimentAnalysis()).thenReturn(List.of(testUser));

        int processed = weeklySentimentService.runWeeklySentimentReport();

        assertThat(processed).isEqualTo(1); // processed but no email sent
        verify(sentimentKafkaProducer, never()).sendWeeklySentiment(any());
    }

    @Test
    void runWeeklySentimentReport_shouldFallbackToEmail_whenKafkaFails() {
        when(userRepoImpl.getUsersForSentimentAnalysis()).thenReturn(List.of(testUser));
        doThrow(new RuntimeException("Kafka down")).when(sentimentKafkaProducer).sendWeeklySentiment(any());

        int processed = weeklySentimentService.runWeeklySentimentReport();

        assertThat(processed).isEqualTo(1);
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void runWeeklySentimentReport_shouldContinueProcessing_whenOneUserFails() {
        User badUser = User.builder()
                .userId(UUID.randomUUID())
                .userName("baduser")
                .password("encoded")
                .email("bad@example.com")
                .sentimentAnalysis(true)
                .journalEntries(null) // will cause NPE
                .build();

        when(userRepoImpl.getUsersForSentimentAnalysis()).thenReturn(List.of(badUser, testUser));

        int processed = weeklySentimentService.runWeeklySentimentReport();

        // badUser fails, testUser succeeds
        assertThat(processed).isEqualTo(1);
    }

    @Test
    void runWeeklySentimentReport_shouldSendCorrectEmailContent() {
        when(userRepoImpl.getUsersForSentimentAnalysis()).thenReturn(List.of(testUser));

        weeklySentimentService.runWeeklySentimentReport();

        ArgumentCaptor<SentimentData> captor = ArgumentCaptor.forClass(SentimentData.class);
        verify(sentimentKafkaProducer).sendWeeklySentiment(captor.capture());

        SentimentData data = captor.getValue();
        assertThat(data.getEmail()).isEqualTo("test@example.com");
        assertThat(data.getSentiment()).contains("testuser");
        assertThat(data.getSentiment()).contains("HAPPY");
    }
}
