package com.trishal.journalApp.api.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class GeminiResponseTest {

    @Test
    void getFirstText_shouldReturnText_whenCandidatesExist() {
        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText("HAPPY");
        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(List.of(part));
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of(candidate));

        assertThat(response.getFirstText()).isEqualTo("HAPPY");
    }

    @Test
    void getFirstText_shouldReturnNull_whenCandidatesIsNull() {
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(null);

        assertThat(response.getFirstText()).isNull();
    }

    @Test
    void getFirstText_shouldReturnNull_whenCandidatesIsEmpty() {
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of());

        assertThat(response.getFirstText()).isNull();
    }

    @Test
    void getFirstText_shouldReturnNull_whenContentIsNull() {
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(null);
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of(candidate));

        assertThat(response.getFirstText()).isNull();
    }

    @Test
    void getFirstText_shouldReturnNull_whenPartsIsNull() {
        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(null);
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of(candidate));

        assertThat(response.getFirstText()).isNull();
    }

    @Test
    void getFirstText_shouldReturnNull_whenPartsIsEmpty() {
        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(List.of());
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of(candidate));

        assertThat(response.getFirstText()).isNull();
    }
}
