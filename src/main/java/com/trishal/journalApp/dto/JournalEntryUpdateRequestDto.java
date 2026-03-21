package com.trishal.journalApp.dto;

import com.trishal.journalApp.enums.Sentiment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryUpdateRequestDto {

    private String title;
    private String content;
    private Sentiment sentiment;
}