package com.trishal.journalApp.service;

import com.trishal.journalApp.api.request.GeminiRequest;
import com.trishal.journalApp.api.response.GeminiResponse;
import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String PROMPT_TEMPLATE =
            "Classify the sentiment of this journal entry in one word: HAPPY, SAD, ANGRY, or ANXIOUS. " +
                    "Reply with only that word. Entry: \"%s\"";

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private AppCache appCache;

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

            String geminiUrl = appCache.appCache.get(AppCache.keys.GEMINI_API.toString());

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
                    geminiUrl, HttpMethod.POST, entity, GeminiResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(response.getBody())) {
                String rawText = response.getBody().getFirstText();
                return parseSentiment(rawText);
            }

        } catch (JournalAppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini sentiment analysis failed for text snippet '{}...': {}",
                    text.length() > 50 ? text.substring(0, 50) : text, e.getMessage(), e);
            throw new JournalAppException(ErrorCode.GEMINI_SERVICE_UNAVAILABLE, e);
        }
        return null;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Sentiment parseSentiment(String raw) {
        if (StringUtils.isEmpty(raw)) {
            throw new JournalAppException(ErrorCode.SENTIMENT_ANALYSIS_FAILED, "Gemini returned empty response");
        }
        String cleaned = raw.trim().toUpperCase().replaceAll("[^A-Z]", "");
        try {
            return Sentiment.valueOf(cleaned);
        } catch (IllegalArgumentException e) {
            log.warn("Gemini returned unrecognised sentiment value: '{}'. Defaulting to null.", raw);
            throw new JournalAppException(ErrorCode.SENTIMENT_INVALID, "value: " + raw);
        }
    }

    /** Strip quotes so the entry text does not break the prompt. */
    private String sanitise(String text) {
        if (StringUtils.isEmpty(text)) return "";
        return text.replace("\"", "'").replace("\n", " ");
    }
}