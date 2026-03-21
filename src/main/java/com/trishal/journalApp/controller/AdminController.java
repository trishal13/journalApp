package com.trishal.journalApp.controller;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.dto.MessageResponseDto;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeeklySentimentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

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

    @GetMapping("/all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> allUsers = userService.getAll();
        if (!Objects.isNull(allUsers) && !allUsers.isEmpty()) {
            return new ResponseEntity<>(allUsers, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/create-admin-user")
    public ResponseEntity<User> createAdmin(@RequestBody User user) {
        userService.saveAdmin(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/clear-app-cache")
    public ResponseEntity<MessageResponseDto> clearAppCache() {
        appCache.init();
        return new ResponseEntity<>(
                MessageResponseDto.builder()
                        .message("App cache refreshed successfully.")
                        .success(true)
                        .build(),
                HttpStatus.OK
        );
    }

    /**
     * Force-runs the weekly sentiment cron job immediately.
     * Only accessible by ADMIN role (enforced in SecurityConfig).
     */
    @PostMapping("/trigger-weekly-sentiment")
    public ResponseEntity<MessageResponseDto> triggerWeeklySentimentReport() {
        log.info("Admin manually triggered weekly sentiment report.");
        int processed = weeklySentimentService.runWeeklySentimentReport();
        return new ResponseEntity<>(
                MessageResponseDto.builder()
                        .message("Weekly sentiment report triggered. Processed " + processed + " users.")
                        .success(true)
                        .build(),
                HttpStatus.OK
        );
    }
}