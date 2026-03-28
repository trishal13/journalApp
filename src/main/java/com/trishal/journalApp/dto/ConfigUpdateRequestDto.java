package com.trishal.journalApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigUpdateRequestDto {

    @NotBlank(message = "Value is required")
    @Size(max = 255, message = "Value cannot exceed 255 characters")
    private String value;
}