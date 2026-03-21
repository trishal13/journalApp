package com.trishal.journalApp.controller;

import com.trishal.journalApp.dto.JournalEntryCreateRequestDto;
import com.trishal.journalApp.dto.JournalEntryResponseDto;
import com.trishal.journalApp.dto.JournalEntryUpdateRequestDto;
import com.trishal.journalApp.dto.MessageResponseDto;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.exception.JournalEntryAccessDeniedException;
import com.trishal.journalApp.exception.JournalEntryNotFoundException;
import com.trishal.journalApp.mapper.JournalEntryMapper;
import com.trishal.journalApp.service.JournalEntryService;
import com.trishal.journalApp.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/journal")
public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private UserService userService;

    @Autowired
    private JournalEntryMapper journalEntryMapper;

    // ── GET all ───────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<JournalEntryResponseDto>> getAllJournalEntriesOfUser() {
        User user = getAuthenticatedUser();
        List<JournalEntry> journalEntries = user.getJournalEntries();
        if (journalEntries == null || journalEntries.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(journalEntryMapper.toResponseList(journalEntries));
    }

    // ── POST create ───────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<JournalEntryResponseDto> createEntry(
            @Valid @RequestBody JournalEntryCreateRequestDto journalEntryCreateRequestDto) {

        String userName = getAuthenticatedUserName();
        JournalEntry newJournalEntry = journalEntryMapper.toEntity(journalEntryCreateRequestDto);
        // saveEntry calls Gemini and sets sentiment automatically
        journalEntryService.saveEntry(newJournalEntry, userName);
        return new ResponseEntity<>(journalEntryMapper.toResponse(newJournalEntry), HttpStatus.CREATED);
    }

    // ── GET by id ─────────────────────────────────────────────────────────────

    @GetMapping("/id/{id}")
    public ResponseEntity<JournalEntryResponseDto> getJournalEntryById(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        assertEntryBelongsToUser(user, id);

        JournalEntry journalEntry = journalEntryService.getJournalEntryById(id)
                .orElseThrow(() -> new JournalEntryNotFoundException(id));

        return ResponseEntity.ok(journalEntryMapper.toResponse(journalEntry));
    }

    // ── DELETE by id ──────────────────────────────────────────────────────────

    @DeleteMapping("/id/{id}")
    public ResponseEntity<MessageResponseDto> deleteJournalEntryById(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        assertEntryBelongsToUser(user, id);

        journalEntryService.deleteJournalEntryById(id, user.getUserName());
        return ResponseEntity.ok(MessageResponseDto.builder()
                .message("Journal entry deleted successfully.")
                .success(true)
                .build());
    }

    // ── PUT update ────────────────────────────────────────────────────────────

    @PutMapping("/id/{id}")
    public ResponseEntity<JournalEntryResponseDto> updateJournalEntryById(
            @PathVariable UUID id,
            @RequestBody JournalEntryUpdateRequestDto dto) {

        User user = getAuthenticatedUser();
        assertEntryBelongsToUser(user, id);

        JournalEntry existing = journalEntryService.getJournalEntryById(id)
                .orElseThrow(() -> new JournalEntryNotFoundException(id));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            existing.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null && !dto.getContent().isBlank()) {
            existing.setContent(dto.getContent());
        }
        // saveEntry re-runs Gemini sentiment analysis on the updated content
        journalEntryService.saveEntry(existing);

        return ResponseEntity.ok(journalEntryMapper.toResponse(existing));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String getAuthenticatedUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private User getAuthenticatedUser() {
        return userService.findByUserName(getAuthenticatedUserName());
    }

    /**
     * Throws {@link JournalEntryAccessDeniedException} if the entry does not
     * belong to the given user. This prevents users from accessing each other's entries.
     */
    private void assertEntryBelongsToUser(User user, UUID entryId) {
        boolean owned = user.getJournalEntries()
                .stream()
                .anyMatch(je -> je.getId().equals(entryId));
        if (!owned) {
            throw new JournalEntryAccessDeniedException(entryId);
        }
    }
}