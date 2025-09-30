package com.trishal.journalApp.dto;

import com.trishal.journalApp.enums.Sentiment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryUpdateRequestDto {

    private String title;
    private String content;
    private Sentiment sentiment;

}
