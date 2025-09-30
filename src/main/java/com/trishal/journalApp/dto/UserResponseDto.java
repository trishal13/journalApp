package com.trishal.journalApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDto {

    private UUID userId;
    private String userName;
    private String email;
    private boolean sentimentAnalysis;
    private List<String> roles;
    private int journalEntryCount;

}
