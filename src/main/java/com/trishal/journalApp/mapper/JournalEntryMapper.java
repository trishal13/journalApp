package com.trishal.journalApp.mapper;

import com.trishal.journalApp.dto.JournalEntryCreateRequestDto;
import com.trishal.journalApp.dto.JournalEntryResponseDto;
import com.trishal.journalApp.entity.JournalEntry;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JournalEntryMapper {

    /**
     * Maps a create-request DTO → JournalEntry entity using the builder.
     * Note: user and date are intentionally omitted here — the service layer
     * sets them after ownership is verified.
     */
    public JournalEntry toEntity(JournalEntryCreateRequestDto dto) {
        return JournalEntry.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .sentiment(dto.getSentiment())
                .build();
    }

    public JournalEntryResponseDto toResponse(JournalEntry journalEntry) {
        return JournalEntryResponseDto.builder()
                .id(journalEntry.getId())
                .title(journalEntry.getTitle())
                .content(journalEntry.getContent())
                .date(journalEntry.getDate())
                .sentiment(journalEntry.getSentiment())
                .authorUserName(!ObjectUtils.isEmpty(journalEntry.getUser())
                        ? journalEntry.getUser().getUserName()
                        : null)
                .build();
    }

    public List<JournalEntryResponseDto> toResponseList(List<JournalEntry> journalEntries) {
        return journalEntries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}