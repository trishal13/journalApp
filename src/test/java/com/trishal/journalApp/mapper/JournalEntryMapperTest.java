package com.trishal.journalApp.mapper;

import com.trishal.journalApp.dto.JournalEntryCreateRequestDto;
import com.trishal.journalApp.dto.JournalEntryResponseDto;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JournalEntryMapperTest {

    private final JournalEntryMapper mapper = new JournalEntryMapper();

    @Test
    void toEntity_shouldMapTitleContentAndSentiment() {
        JournalEntryCreateRequestDto dto = JournalEntryCreateRequestDto.builder()
                .title("My Day")
                .content("It was great")
                .sentiment(Sentiment.HAPPY)
                .build();

        JournalEntry entity = mapper.toEntity(dto);

        assertThat(entity.getTitle()).isEqualTo("My Day");
        assertThat(entity.getContent()).isEqualTo("It was great");
        assertThat(entity.getSentiment()).isEqualTo(Sentiment.HAPPY);
        // user and date should not be set by mapper
        assertThat(entity.getUser()).isNull();
        assertThat(entity.getDate()).isNull();
    }

    @Test
    void toResponse_shouldMapAllFieldsIncludingAuthor() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .userName("author")
                .password("encoded")
                .build();

        UUID entryId = UUID.randomUUID();
        Date now = new Date();
        JournalEntry entry = JournalEntry.builder()
                .id(entryId)
                .title("Title")
                .content("Content")
                .date(now)
                .sentiment(Sentiment.SAD)
                .user(user)
                .build();

        JournalEntryResponseDto response = mapper.toResponse(entry);

        assertThat(response.getId()).isEqualTo(entryId);
        assertThat(response.getTitle()).isEqualTo("Title");
        assertThat(response.getContent()).isEqualTo("Content");
        assertThat(response.getDate()).isEqualTo(now);
        assertThat(response.getSentiment()).isEqualTo(Sentiment.SAD);
        assertThat(response.getAuthorUserName()).isEqualTo("author");
    }

    @Test
    void toResponse_shouldReturnNullAuthor_whenUserIsNull() {
        JournalEntry entry = JournalEntry.builder()
                .id(UUID.randomUUID())
                .title("Orphan")
                .build();

        JournalEntryResponseDto response = mapper.toResponse(entry);

        assertThat(response.getAuthorUserName()).isNull();
    }

    @Test
    void toResponseList_shouldMapAllEntries() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .userName("author")
                .password("encoded")
                .build();

        List<JournalEntry> entries = List.of(
                JournalEntry.builder().id(UUID.randomUUID()).title("A").user(user).build(),
                JournalEntry.builder().id(UUID.randomUUID()).title("B").user(user).build(),
                JournalEntry.builder().id(UUID.randomUUID()).title("C").user(user).build()
        );

        List<JournalEntryResponseDto> result = mapper.toResponseList(entries);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(JournalEntryResponseDto::getTitle).containsExactly("A", "B", "C");
    }

    @Test
    void toResponseList_shouldReturnEmptyList_whenInputIsEmpty() {
        assertThat(mapper.toResponseList(List.of())).isEmpty();
    }
}
