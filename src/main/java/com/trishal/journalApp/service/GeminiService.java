package com.trishal.journalApp.service;

import com.trishal.journalApp.api.request.GeminiRequest;
import com.trishal.journalApp.api.response.GeminiResponse;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Calls the Gemini Flash API to determine the sentiment of a journal entry.
 * The model is asked to respond with exactly one word: HAPPY | SAD | ANGRY | ANXIOUS.
 */
@Slf4j
@Service
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private static final String PROMPT_TEMPLATE =
            "Analyze the sentiment of the following journal entry text and respond with " +
                    "EXACTLY ONE WORD from this list: HAPPY, SAD, ANGRY, ANXIOUS. " +
                    "Do not include any explanation, punctuation, or extra text. " +
                    "Journal entry: \"%s\"";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GeminiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Analyse the sentiment of a journal entry text.
     *
     * @param text the journal entry title + content combined
     * @return the detected {@link Sentiment}, or {@code null} if analysis fails gracefully
     */
    public Sentiment analyseSentiment(String text) {
        try {
            String prompt = String.format(PROMPT_TEMPLATE, sanitise(text));

            GeminiRequest requestBody = GeminiRequest.builder()
                    .contents(List.of(
                            GeminiRequest.Content.builder()
                                    .parts(List.of(
                                            GeminiRequest.Part.builder().text(prompt).build()
                                    ))
                                    .build()
                    ))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                    GEMINI_URL, HttpMethod.POST, entity, GeminiResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String rawText = response.getBody().getFirstText();
                return parseSentiment(rawText);
            }

        } catch (Exception e) {
            log.error("Gemini sentiment analysis failed for text snippet '{}...': {}",
                    text.length() > 50 ? text.substring(0, 50) : text, e.getMessage(), e);
            // We throw so the caller can decide whether to surface the error or swallow it.
            throw new JournalAppException(ErrorCode.GEMINI_SERVICE_UNAVAILABLE, e);
        }
        return null;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Sentiment parseSentiment(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim().toUpperCase().replaceAll("[^A-Z]", "");
        try {
            return Sentiment.valueOf(cleaned);
        } catch (IllegalArgumentException e) {
            log.warn("Gemini returned unrecognised sentiment value: '{}'. Defaulting to null.", raw);
            return null;
        }
    }

    /** Strip quotes so the entry text does not break the prompt. */
    private String sanitise(String text) {
        if (text == null) return "";
        return text.replace("\"", "'").replace("\n", " ");
    }
}