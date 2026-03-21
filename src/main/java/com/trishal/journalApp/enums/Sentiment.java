package com.trishal.journalApp.enums;

/**
 * The four sentiments the Gemini AI can assign to a journal entry.
 * The exact string values are what Gemini is prompted to return,
 * so do not rename them without updating the prompt in GeminiService.
 */
public enum Sentiment {
    HAPPY,
    SAD,
    ANGRY,
    ANXIOUS
}