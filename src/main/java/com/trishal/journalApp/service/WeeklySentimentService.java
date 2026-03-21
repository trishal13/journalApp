package com.trishal.journalApp.service;

import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.kafka.producer.SentimentKafkaProducer;
import com.trishal.journalApp.model.SentimentData;
import com.trishal.journalApp.repository.impl.UserRepoImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains ALL logic for the weekly sentiment report.
 * Both the scheduler and the admin "force-run" endpoint delegate here.
 */
@Slf4j
@Service
public class WeeklySentimentService {

    @Autowired
    private UserRepoImpl userRepoImpl;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SentimentKafkaProducer sentimentKafkaProducer;

    /**
     * Core logic:
     * 1. Load all users with sentimentAnalysis=true and a valid email.
     * 2. For each user, collect journal entries from the last 7 days.
     * 3. Find the most-frequent sentiment.
     * 4. Build a human-readable summary and publish to Kafka (falls back to direct email).
     *
     * @return number of users processed
     */
    public int runWeeklySentimentReport() {
        List<User> users = userRepoImpl.getUsersForSentimentAnalysis();
        log.info("Weekly sentiment report starting for {} eligible users", users.size());

        int processed = 0;
        for (User user : users) {
            try {
                processUser(user);
                processed++;
            } catch (Exception e) {
                // Do not let one bad user abort the whole batch
                log.error("Failed to process sentiment for user={}: {}", user.getUserName(), e.getMessage(), e);
            }
        }

        log.info("Weekly sentiment report complete. Processed {}/{} users.", processed, users.size());
        return processed;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void processUser(User user) {
        List<JournalEntry> recentEntries = getEntriesFromLastSevenDays(user);

        if (recentEntries.isEmpty()) {
            log.debug("No entries in last 7 days for user={}", user.getUserName());
            return;
        }

        Optional<Sentiment> dominantSentiment = findDominantSentiment(recentEntries);
        if (dominantSentiment.isEmpty()) {
            log.debug("No sentiments recorded this week for user={}", user.getUserName());
            return;
        }

        String summaryMessage = buildSummaryMessage(user, recentEntries, dominantSentiment.get());
        SentimentData sentimentData = SentimentData.builder()
                .email(user.getEmail())
                .sentiment(summaryMessage)
                .build();

        publishOrFallback(sentimentData);
    }

    private List<JournalEntry> getEntriesFromLastSevenDays(User user) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        return user.getJournalEntries()
                .stream()
                .filter(entry -> entry.getDate() != null &&
                        entry.getDate().toInstant()
                                .isAfter(sevenDaysAgo.atZone(ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());
    }

    private Optional<Sentiment> findDominantSentiment(List<JournalEntry> entries) {
        Map<Sentiment, Long> counts = entries.stream()
                .filter(e -> e.getSentiment() != null)
                .collect(Collectors.groupingBy(JournalEntry::getSentiment, Collectors.counting()));

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /**
     * Human-readable weekly summary with entry count and sentiment breakdown.
     */
    private String buildSummaryMessage(User user, List<JournalEntry> entries, Sentiment dominant) {
        Map<Sentiment, Long> counts = entries.stream()
                .filter(e -> e.getSentiment() != null)
                .collect(Collectors.groupingBy(JournalEntry::getSentiment, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(user.getUserName()).append(",\n\n");
        sb.append("Here is your weekly journal sentiment summary:\n\n");
        sb.append("  • Total entries this week: ").append(entries.size()).append("\n");
        sb.append("  • Dominant mood: ").append(dominant).append("\n\n");
        sb.append("Mood breakdown:\n");
        counts.forEach((sentiment, count) ->
                sb.append("  • ").append(sentiment).append(": ").append(count).append(" entries\n"));
        sb.append("\nKeep journaling — see you next week!");
        return sb.toString();
    }

    private void publishOrFallback(SentimentData sentimentData) {
        try {
            sentimentKafkaProducer.sendWeeklySentiment(sentimentData);
        } catch (Exception e) {
            log.warn("Kafka unavailable, falling back to direct email for {}: {}",
                    sentimentData.getEmail(), e.getMessage());
            emailService.sendEmail(
                    sentimentData.getEmail(),
                    "Your Weekly Sentiment Report",
                    sentimentData.getSentiment()
            );
        }
    }
}