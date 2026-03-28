package com.trishal.journalApp.service;

import com.trishal.journalApp.api.response.GeminiResponse;
import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.exception.JournalAppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AppCache appCache;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(restTemplate);
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(geminiService, "appCache", appCache);
        Map<String, String> cacheMap = new HashMap<>();
        cacheMap.put("GEMINI_API", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent");
        appCache.appCache = cacheMap;
    }

    private GeminiResponse buildGeminiResponse(String text) {
        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText(text);
        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(List.of(part));
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);
        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of(candidate));
        return response;
    }

    @Test
    void analyseSentiment_shouldReturnHappy_whenGeminiReturnsHAPPY() {
        GeminiResponse geminiResponse = buildGeminiResponse("HAPPY");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(geminiResponse, HttpStatus.OK));

        Sentiment result = geminiService.analyseSentiment("I had a great day");

        assertThat(result).isEqualTo(Sentiment.HAPPY);
    }

    @Test
    void analyseSentiment_shouldReturnSad_whenGeminiReturnsSAD() {
        GeminiResponse geminiResponse = buildGeminiResponse("SAD");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(geminiResponse, HttpStatus.OK));

        Sentiment result = geminiService.analyseSentiment("I feel terrible today");

        assertThat(result).isEqualTo(Sentiment.SAD);
    }

    @Test
    void analyseSentiment_shouldReturnAngry() {
        GeminiResponse geminiResponse = buildGeminiResponse("ANGRY");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(geminiResponse, HttpStatus.OK));

        assertThat(geminiService.analyseSentiment("So frustrated")).isEqualTo(Sentiment.ANGRY);
    }

    @Test
    void analyseSentiment_shouldReturnAnxious() {
        GeminiResponse geminiResponse = buildGeminiResponse("ANXIOUS");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(geminiResponse, HttpStatus.OK));

        assertThat(geminiService.analyseSentiment("I'm worried about tomorrow")).isEqualTo(Sentiment.ANXIOUS);
    }

    @Test
    void analyseSentiment_shouldThrowJournalAppException_whenGeminiReturnsUnrecognisedValue() {
        GeminiResponse geminiResponse = buildGeminiResponse("CONFUSED");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(geminiResponse, HttpStatus.OK));

        assertThatThrownBy(() -> geminiService.analyseSentiment("Some text"))
                .isInstanceOf(JournalAppException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void analyseSentiment_shouldHandleExtraWhitespaceAndPunctuation() {
        GeminiResponse geminiResponse = buildGeminiResponse("  HAPPY.  ");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(geminiResponse, HttpStatus.OK));

        assertThat(geminiService.analyseSentiment("Great day")).isEqualTo(Sentiment.HAPPY);
    }

    @Test
    void analyseSentiment_shouldReturnNull_whenResponseBodyIsNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThat(geminiService.analyseSentiment("Some text")).isNull();
    }

    @Test
    void analyseSentiment_shouldThrowJournalAppException_whenRestTemplateFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> geminiService.analyseSentiment("Some text"))
                .isInstanceOf(JournalAppException.class);
    }

    @Test
    void analyseSentiment_shouldSanitiseQuotesInInput() {
        GeminiResponse geminiResponse = buildGeminiResponse("HAPPY");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(new ResponseEntity<>(geminiResponse, HttpStatus.OK));

        // Should not throw even with quotes in input
        Sentiment result = geminiService.analyseSentiment("He said \"hello\" to me");
        assertThat(result).isEqualTo(Sentiment.HAPPY);
    }
}
