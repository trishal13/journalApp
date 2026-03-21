package com.trishal.journalApp.service;

import org.springframework.stereotype.Service;

/**
 * Legacy stub — sentiment analysis is now handled by {@link GeminiService}.
 * Kept to avoid breaking any existing references. Safe to remove once fully migrated.
 */
@Service
public class SentimentAnalysisService {

    /**
     * @deprecated Use {@link GeminiService#analyseSentiment(String)} instead.
     */
    @Deprecated
    public String getSentiment(String text) {
        return "";
    }
}