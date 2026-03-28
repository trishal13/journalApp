package com.trishal.journalApp.controller;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.dto.*;
import com.trishal.journalApp.entity.ConfigJournalApp;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.mapper.JournalEntryMapper;
import com.trishal.journalApp.mapper.UserMapper;
import com.trishal.journalApp.service.ConfigService;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeeklySentimentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppCache appCache;

    @Autowired
    private WeeklySentimentService weeklySentimentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JournalEntryMapper journalEntryMapper;

    // ── User management ───────────────────────────────────────────────────────

    @GetMapping("/all-users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> allUsers = userService.getAll();
        if (!ObjectUtils.isEmpty(allUsers)) {
            return ResponseEntity.ok(ApiResponse.success(allUsers, "Users retrieved."));
        }
        return new ResponseEntity<>(ApiResponse.error("No users found."), HttpStatus.NOT_FOUND);
    }

    @PostMapping("/create-admin-user")
    public ResponseEntity<ApiResponse<User>> createAdmin(@RequestBody User user) {
        userService.saveAdmin(user);
        return ResponseEntity.ok(ApiResponse.success(user, "Admin user created."));
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    @GetMapping("/clear-app-cache")
    public ResponseEntity<ApiResponse<Void>> clearAppCache() {
        appCache.init();
        return ResponseEntity.ok(ApiResponse.success("App cache refreshed successfully."));
    }

    // ── Sentiment ─────────────────────────────────────────────────────────────

    @PostMapping("/trigger-weekly-sentiment")
    public ResponseEntity<ApiResponse<Void>> triggerWeeklySentimentReport() {
        log.info("Admin manually triggered weekly sentiment report.");
        int processed = weeklySentimentService.runWeeklySentimentReport();
        return ResponseEntity.ok(
                ApiResponse.success("Weekly sentiment report triggered. Processed "
                        + processed + " users."));
    }

    // ── Config management ─────────────────────────────────────────────────────

    /**
     * List all entries in config_journal_app.
     * Returns a DTO so the entity internals (generated UUID type, etc.) are
     * not coupled to the API contract.
     */
    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<ConfigResponseDto>>> getAllConfigs() {
        List<ConfigJournalApp> configs = configService.getAllConfigs();
        if (ObjectUtils.isEmpty(configs)) {
            return new ResponseEntity<>(
                    ApiResponse.error("No configs found."), HttpStatus.NOT_FOUND);
        }
        List<ConfigResponseDto> response = configs.stream()
                .map(c -> ConfigResponseDto.builder()
                        .id(c.getId())
                        .key(c.getKey())
                        .value(c.getValue())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response, "Configs retrieved."));
    }

    /**
     * Update the value of an existing config entry by key.
     *
     * The key is taken from the URL path — this makes it explicit and
     * prevents accidentally updating the wrong entry.
     * AppCache is refreshed immediately inside ConfigService after the DB update.
     *
     * Example: PUT /admin/configs/WEATHER_API
     * Body:    { "value": "https://..." }
     */
    @PutMapping("/configs/{key}")
    public ResponseEntity<ApiResponse<ConfigResponseDto>> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody ConfigUpdateRequestDto dto) {

        ConfigJournalApp updated = configService.updateConfig(key, dto);
        ConfigResponseDto response = ConfigResponseDto.builder()
                .id(updated.getId())
                .key(updated.getKey())
                .value(updated.getValue())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, "Config updated and cache refreshed."));
    }

    /**
     * Update any user's profile by username.
     * PUT /admin/users/{username}
     */
    @PutMapping("/users/{username}")
    public ResponseEntity<ApiResponse<UserUpdateResponseDto>> updateUser(
            @PathVariable String username,
            @Valid @RequestBody UserUpdateRequestDto dto) {

        log.info("Admin updating user: {}", username);
        User existingUser = userService.findByUserName(username);
        User updated = userService.updateUser(existingUser, dto);
        return ResponseEntity.ok(
                ApiResponse.success(userMapper.toUpdateResponse(updated), "User updated successfully."));
    }

    /**
     * Get all journal entries for a specific user.
     * GET /admin/users/{username}/journals
     */
    @GetMapping("/users/{username}/journals")
    public ResponseEntity<ApiResponse<List<JournalEntryResponseDto>>> getUserJournals(
            @PathVariable String username) {

        log.info("Admin fetching journals for user: {}", username);
        User user = userService.findByUserName(username);
        List<JournalEntry> entries = user.getJournalEntries();

        if (ObjectUtils.isEmpty(entries)) {
            return ResponseEntity.ok(ApiResponse.success(List.of(), "No journal entries found."));
        }

        return ResponseEntity.ok(
                ApiResponse.success(journalEntryMapper.toResponseList(entries), "Journal entries retrieved."));
    }
}