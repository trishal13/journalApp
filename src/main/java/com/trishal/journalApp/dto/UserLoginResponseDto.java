package com.trishal.journalApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDto {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private String userName;
    private List<String> roles;
    private long expiresIn;
}