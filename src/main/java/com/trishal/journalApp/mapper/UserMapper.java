package com.trishal.journalApp.mapper;

import com.trishal.journalApp.dto.UserRegistrationRequestDto;
import com.trishal.journalApp.dto.UserResponseDto;
import com.trishal.journalApp.entity.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserMapper {

    /**
     * Maps a registration DTO → User entity using the builder.
     * Note: password encoding and role assignment happen in UserService,
     * not here — the mapper is only responsible for field mapping.
     */
    public User toEntity(UserRegistrationRequestDto request) {
        return User.builder()
                .userName(request.getUserName())
                .password(request.getPassword())   // raw — encoded in UserService
                .email(request.getEmail())
                .sentimentAnalysis(request.isSentimentAnalysis())
                .build();
    }

    public UserResponseDto toResponse(User user) {
        // BUG FIX: was `Objects.isNull(...)` which returned 0 when list existed
        int entryCount = Objects.nonNull(user.getJournalEntries())
                ? user.getJournalEntries().size()
                : 0;

        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .sentimentAnalysis(user.isSentimentAnalysis())
                .roles(user.getRoles())
                .journalEntryCount(entryCount)
                .build();
    }
}