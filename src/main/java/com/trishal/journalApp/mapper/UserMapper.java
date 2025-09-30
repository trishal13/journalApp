package com.trishal.journalApp.mapper;

import com.trishal.journalApp.dto.UserRegistrationRequestDto;
import com.trishal.journalApp.dto.UserResponseDto;
import com.trishal.journalApp.entity.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserMapper {

    public User toEntity(UserRegistrationRequestDto request) {
        User user = new User();
        user.setUserName(request.getUserName());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setSentimentAnalysis(request.isSentimentAnalysis());
        return user;
    }

    public UserResponseDto toResponse(User user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .sentimentAnalysis(user.isSentimentAnalysis())
                .roles(user.getRoles())
                .journalEntryCount(Objects.isNull(user.getJournalEntries()) ? user.getJournalEntries().size() : 0)
                .build();
    }

}
