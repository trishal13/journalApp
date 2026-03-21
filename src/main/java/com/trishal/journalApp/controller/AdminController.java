package com.trishal.journalApp.controller;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.dto.ApiResponse;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeeklySentimentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/clear-app-cache")
    public ResponseEntity<ApiResponse<Void>> clearAppCache() {
        appCache.init();
        return ResponseEntity.ok(ApiResponse.success("App cache refreshed successfully."));
    }

    @PostMapping("/trigger-weekly-sentiment")
    public ResponseEntity<ApiResponse<Void>> triggerWeeklySentimentReport() {
        log.info("Admin manually triggered weekly sentiment report.");
        int processed = weeklySentimentService.runWeeklySentimentReport();
        return ResponseEntity.ok(
                ApiResponse.success("Weekly sentiment report triggered. Processed " + processed + " users."));
    }
}
