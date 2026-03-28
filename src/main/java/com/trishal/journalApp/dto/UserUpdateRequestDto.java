package com.trishal.journalApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDto {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String userName;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Email(message = "Please provide a valid email address")
    private String email;

    private Boolean sentimentAnalysis;
}