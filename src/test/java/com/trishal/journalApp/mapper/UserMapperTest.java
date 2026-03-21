package com.trishal.journalApp.mapper;

import com.trishal.journalApp.dto.UserRegistrationRequestDto;
import com.trishal.journalApp.dto.UserResponseDto;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toEntity_shouldMapAllFields() {
        UserRegistrationRequestDto dto = UserRegistrationRequestDto.builder()
                .userName("newuser")
                .password("secret123")
                .email("new@example.com")
                .sentimentAnalysis(true)
                .build();

        User user = userMapper.toEntity(dto);

        assertThat(user.getUserName()).isEqualTo("newuser");
        assertThat(user.getPassword()).isEqualTo("secret123"); // raw, not encoded
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.isSentimentAnalysis()).isTrue();
    }

    @Test
    void toResponse_shouldMapAllFieldsIncludingEntryCount() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .userName("testuser")
                .password("encoded")
                .email("test@example.com")
                .sentimentAnalysis(true)
                .roles(List.of("USER"))
                .journalEntries(new ArrayList<>(List.of(
                        JournalEntry.builder().id(UUID.randomUUID()).title("Entry 1").build(),
                        JournalEntry.builder().id(UUID.randomUUID()).title("Entry 2").build()
                )))
                .build();

        UserResponseDto response = userMapper.toResponse(user);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUserName()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.isSentimentAnalysis()).isTrue();
        assertThat(response.getRoles()).containsExactly("USER");
        assertThat(response.getJournalEntryCount()).isEqualTo(2);
    }

    @Test
    void toResponse_shouldReturnZeroEntryCount_whenJournalEntriesIsNull() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .userName("testuser")
                .password("encoded")
                .build();
        // Force null entries (bypass @Builder.Default)
        user.setJournalEntries(null);

        UserResponseDto response = userMapper.toResponse(user);

        assertThat(response.getJournalEntryCount()).isZero();
    }

    @Test
    void toResponse_shouldNotExposePassword() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .userName("testuser")
                .password("super-secret")
                .build();

        UserResponseDto response = userMapper.toResponse(user);

        // UserResponseDto should not have a password field at all
        assertThat(response).hasNoNullFieldsOrPropertiesExcept("email", "roles");
    }
}
