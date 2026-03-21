package com.trishal.journalApp.service;

import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.exception.JournalEntryNotFoundException;
import com.trishal.journalApp.exception.UserNotFoundException;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.repository.JournalEntryRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class JournalEntryService {

    @Autowired
    private JournalEntryRepo journalEntryRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private GeminiService geminiService;

    // ── Create ───────────────────────────────────────────────────────────────

    /**
     * Persist a new journal entry for the given user.
     * Sentiment is auto-detected via Gemini if not explicitly set.
     */
    @Transactional
    public void saveEntry(JournalEntry journalEntry, String userName) {
        User user = userService.findByUserName(userName);
        if (Objects.isNull(user)) {
            throw new UserNotFoundException(userName);
        }

        // Auto-analyse sentiment via Gemini
        Sentiment detectedSentiment = analyseSentimentSafely(journalEntry);
        journalEntry.setSentiment(detectedSentiment);
        journalEntry.setUser(user);

        try {
            journalEntryRepo.save(journalEntry);
            log.info("Created journal entry id={} for user={} with sentiment={}",
                    journalEntry.getId(), userName, detectedSentiment);
        } catch (Exception e) {
            log.error("Failed to save journal entry for user={}", userName, e);
            throw new JournalAppException(ErrorCode.JOURNAL_ENTRY_CREATION_FAILED, e);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Persist an existing (updated) journal entry.
     * Re-analyses sentiment whenever title or content may have changed.
     */
    @Transactional
    public void saveEntry(JournalEntry journalEntry) {
        // Re-run sentiment analysis on update
        Sentiment detectedSentiment = analyseSentimentSafely(journalEntry);
        journalEntry.setSentiment(detectedSentiment);

        try {
            journalEntryRepo.save(journalEntry);
            log.info("Updated journal entry id={} with sentiment={}", journalEntry.getId(), detectedSentiment);
        } catch (Exception e) {
            log.error("Failed to update journal entry id={}", journalEntry.getId(), e);
            throw new JournalAppException(ErrorCode.JOURNAL_ENTRY_UPDATE_FAILED, e);
        }
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public List<JournalEntry> getAll() {
        return journalEntryRepo.findAll();
    }

    public Optional<JournalEntry> getJournalEntryById(UUID id) {
        return journalEntryRepo.findById(id);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public boolean deleteJournalEntryById(UUID id, String userName) {
        User user = userService.findByUserName(userName);
        if (Objects.isNull(user)) {
            throw new UserNotFoundException(userName);
        }

        boolean removed = user.getJournalEntries().removeIf(je -> je.getId().equals(id));
        if (!removed) {
            throw new JournalEntryNotFoundException(id);
        }

        try {
            userService.saveEntry(user);
            journalEntryRepo.deleteById(id);
            log.info("Deleted journal entry id={} for user={}", id, userName);
        } catch (Exception e) {
            log.error("Failed to delete journal entry id={} for user={}", id, userName, e);
            throw new JournalAppException(ErrorCode.JOURNAL_ENTRY_DELETION_FAILED, e);
        }

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Calls Gemini for sentiment. If Gemini is unavailable, logs a warning and
     * falls back to whatever sentiment was already on the entry (preserves UX).
     */
    private Sentiment analyseSentimentSafely(JournalEntry entry) {
        try {
            String textToAnalyse = buildTextForAnalysis(entry);
            if (textToAnalyse.isBlank()) return entry.getSentiment();
            return geminiService.analyseSentiment(textToAnalyse);
        } catch (Exception e) {
            log.warn("Gemini sentiment analysis failed — keeping existing sentiment. Reason: {}", e.getMessage());
            return entry.getSentiment();
        }
    }

    private String buildTextForAnalysis(JournalEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.getTitle() != null) sb.append(entry.getTitle()).append(". ");
        if (entry.getContent() != null) sb.append(entry.getContent());
        return sb.toString().trim();
    }
}