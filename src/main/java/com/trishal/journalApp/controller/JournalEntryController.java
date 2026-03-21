package com.trishal.journalApp.controller;

import com.trishal.journalApp.dto.*;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.exception.JournalEntryAccessDeniedException;
import com.trishal.journalApp.exception.JournalEntryNotFoundException;
import com.trishal.journalApp.mapper.JournalEntryMapper;
import com.trishal.journalApp.service.JournalEntryService;
import com.trishal.journalApp.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<JournalEntryResponseDto>>> getAllJournalEntriesOfUser() {
        User user = getAuthenticatedUser();
        List<JournalEntry> journalEntries = user.getJournalEntries();
        if (ObjectUtils.isEmpty(journalEntries)) {
            return ResponseEntity.ok(ApiResponse.success(List.of(), "No journal entries found."));
        }
        return ResponseEntity.ok(
                ApiResponse.success(journalEntryMapper.toResponseList(journalEntries), "Journal entries retrieved."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> createEntry(
            @Valid @RequestBody JournalEntryCreateRequestDto journalEntryCreateRequestDto) {

        String userName = getAuthenticatedUserName();
        JournalEntry newJournalEntry = journalEntryMapper.toEntity(journalEntryCreateRequestDto);
        journalEntryService.saveEntry(newJournalEntry, userName);
        return new ResponseEntity<>(
                ApiResponse.success(journalEntryMapper.toResponse(newJournalEntry), "Journal entry created."),
                HttpStatus.CREATED);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> getJournalEntryById(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        assertEntryBelongsToUser(user, id);

        JournalEntry journalEntry = journalEntryService.getJournalEntryById(id)
                .orElseThrow(() -> new JournalEntryNotFoundException(id));

        return ResponseEntity.ok(ApiResponse.success(journalEntryMapper.toResponse(journalEntry)));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJournalEntryById(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        assertEntryBelongsToUser(user, id);

        journalEntryService.deleteJournalEntryById(id, user.getUserName());
        return ResponseEntity.ok(ApiResponse.success("Journal entry deleted successfully."));
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> updateJournalEntryById(
            @PathVariable UUID id,
            @RequestBody JournalEntryUpdateRequestDto dto) {

        User user = getAuthenticatedUser();
        assertEntryBelongsToUser(user, id);

        JournalEntry existing = journalEntryService.getJournalEntryById(id)
                .orElseThrow(() -> new JournalEntryNotFoundException(id));

        if (StringUtils.isNotBlank(dto.getTitle())) {
            existing.setTitle(dto.getTitle());
        }
        if (StringUtils.isNotBlank(dto.getContent())) {
            existing.setContent(dto.getContent());
        }
        journalEntryService.saveEntry(existing);

        return ResponseEntity.ok(
                ApiResponse.success(journalEntryMapper.toResponse(existing), "Journal entry updated."));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String getAuthenticatedUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private User getAuthenticatedUser() {
        return userService.findByUserName(getAuthenticatedUserName());
    }

    private void assertEntryBelongsToUser(User user, UUID entryId) {
        boolean owned = user.getJournalEntries()
                .stream()
                .anyMatch(je -> je.getId().equals(entryId));
        if (!owned) {
            throw new JournalEntryAccessDeniedException(entryId);
        }
    }
}
