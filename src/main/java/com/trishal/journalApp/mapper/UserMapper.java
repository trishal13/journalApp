package com.trishal.journalApp.mapper;

import com.trishal.journalApp.dto.UserRegistrationRequestDto;
import com.trishal.journalApp.dto.UserResponseDto;
import com.trishal.journalApp.dto.UserUpdateResponseDto;
import com.trishal.journalApp.entity.User;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRegistrationRequestDto request) {
        return User.builder()
                .userName(request.getUserName())
                .password(request.getPassword())
                .email(request.getEmail())
                .sentimentAnalysis(request.isSentimentAnalysis())
                .build();
    }

    public UserResponseDto toResponse(User user) {
        int entryCount = !ObjectUtils.isEmpty(user.getJournalEntries())
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

    /**
     * Maps updated User → UserUpdateResponseDto.
     * Intentionally excludes password — never return hashed credentials in a response.
     */
    public UserUpdateResponseDto toUpdateResponse(User user) {
        return UserUpdateResponseDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .sentimentAnalysis(user.isSentimentAnalysis())
                .roles(user.getRoles())
                .build();
    }
}