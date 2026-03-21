package com.trishal.journalApp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ── User errors (1xxx) ──────────────────────────────────────────────────
    USER_NOT_FOUND("ERR_1001", "User not found.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("ERR_1002", "A user with this username already exists.", HttpStatus.CONFLICT),
    USER_CREATION_FAILED("ERR_1003", "Failed to create the user.", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_UPDATE_FAILED("ERR_1004", "Failed to update the user.", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_DELETION_FAILED("ERR_1005", "Failed to delete the user.", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_INVALID_CREDENTIALS("ERR_1006", "Invalid username or password.", HttpStatus.UNAUTHORIZED),

    // ── Journal entry errors (2xxx) ─────────────────────────────────────────
    JOURNAL_ENTRY_NOT_FOUND("ERR_2001", "Journal entry not found.", HttpStatus.NOT_FOUND),
    JOURNAL_ENTRY_ACCESS_DENIED("ERR_2002", "You do not have access to this journal entry.", HttpStatus.FORBIDDEN),
    JOURNAL_ENTRY_CREATION_FAILED("ERR_2003", "Failed to create the journal entry.", HttpStatus.INTERNAL_SERVER_ERROR),
    JOURNAL_ENTRY_UPDATE_FAILED("ERR_2004", "Failed to update the journal entry.", HttpStatus.INTERNAL_SERVER_ERROR),
    JOURNAL_ENTRY_DELETION_FAILED("ERR_2005", "Failed to delete the journal entry.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Sentiment errors (3xxx) ─────────────────────────────────────────────
    SENTIMENT_ANALYSIS_FAILED("ERR_3001", "Sentiment analysis could not be completed.", HttpStatus.INTERNAL_SERVER_ERROR),
    SENTIMENT_INVALID("ERR_3002", "Provided sentiment value is not valid.", HttpStatus.BAD_REQUEST),

    // ── External service errors (4xxx) ──────────────────────────────────────
    WEATHER_SERVICE_UNAVAILABLE("ERR_4001", "Weather service is currently unavailable.", HttpStatus.SERVICE_UNAVAILABLE),
    GEMINI_SERVICE_UNAVAILABLE("ERR_4002", "Gemini AI service is currently unavailable.", HttpStatus.SERVICE_UNAVAILABLE),
    EMAIL_SEND_FAILED("ERR_4003", "Failed to send email notification.", HttpStatus.INTERNAL_SERVER_ERROR),
    KAFKA_PUBLISH_FAILED("ERR_4004", "Failed to publish message to Kafka.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Auth / JWT errors (5xxx) ────────────────────────────────────────────
    JWT_INVALID("ERR_5001", "JWT token is invalid or malformed.", HttpStatus.UNAUTHORIZED),
    JWT_EXPIRED("ERR_5002", "JWT token has expired.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("ERR_5003", "You do not have permission to perform this action.", HttpStatus.FORBIDDEN),

    // ── Validation errors (6xxx) ────────────────────────────────────────────
    VALIDATION_FAILED("ERR_6001", "Request validation failed.", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY("ERR_6002", "Request body is missing or malformed.", HttpStatus.BAD_REQUEST),

    // ── Generic (9xxx) ──────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR("ERR_9001", "An unexpected internal error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code  = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}