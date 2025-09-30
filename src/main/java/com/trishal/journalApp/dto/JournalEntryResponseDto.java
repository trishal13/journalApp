package com.trishal.journalApp.dto;

import com.trishal.journalApp.enums.Sentiment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JournalEntryResponseDto {

    private UUID id;
    private String title;
    private String content;
    private Date date;
    private Sentiment sentiment;
    private String authorUserName;

}
