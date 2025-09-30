package com.trishal.journalApp.mapper;

import com.trishal.journalApp.dto.JournalEntryCreateRequestDto;
import com.trishal.journalApp.dto.JournalEntryResponseDto;
import com.trishal.journalApp.entity.JournalEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JournalEntryMapper {

    public JournalEntry toEntity(JournalEntryCreateRequestDto journalEntryCreateRequestDto){
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setTitle(journalEntryCreateRequestDto.getTitle());
        journalEntry.setContent(journalEntryCreateRequestDto.getContent());
        journalEntry.setSentiment(journalEntryCreateRequestDto.getSentiment());
        return journalEntry;
    }

    public JournalEntryResponseDto toResponse(JournalEntry journalEntry) {
        return JournalEntryResponseDto.builder()
                .id(journalEntry.getId())
                .title(journalEntry.getTitle())
                .content(journalEntry.getContent())
                .date(journalEntry.getDate())
                .sentiment(journalEntry.getSentiment())
                .authorUserName(journalEntry.getUser() != null ? journalEntry.getUser().getUserName() : null)
                .build();
    }

    public List<JournalEntryResponseDto> toResponseList(List<JournalEntry> journalEntries){
        List<JournalEntryResponseDto> journalEntryResponseDtoList = new ArrayList<>();
        for (JournalEntry journalEntry: journalEntries){
            JournalEntryResponseDto journalEntryResponseDto=toResponse(journalEntry);
            journalEntryResponseDtoList.add(journalEntryResponseDto);
        }
        return journalEntryResponseDtoList;
    }

}
