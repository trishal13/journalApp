package com.trishal.journalApp.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trishal.journalApp.model.SentimentData;
import com.trishal.journalApp.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens on the weekly-sentiments topic and dispatches email notifications.
 *
 * FIX 1: @Value fields CANNOT be referenced inside annotation attributes (compile-time constant
 *         requirement). Use "${property}" placeholder strings directly in @KafkaListener.
 * FIX 2: was "{weekly.sentiment.topic}" — missing $ prefix on both @Value annotations.
 * FIX 3: Receives a String (matches StringDeserializer in application.properties) and
 *         deserialises to SentimentData using ObjectMapper — consistent with the producer.
 */
@Slf4j
@Component
public class SentimentKafkaConsumer {

    private static final String EMAIL_SUBJECT = "Your Weekly Sentiment Report";

    @Autowired
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics    = "${weekly.sentiment.topic}",   // FIX: property placeholder directly in annotation
            groupId   = "${weekly.sentiment.group}",   // FIX: property placeholder directly in annotation
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            // Deserialise JSON string → SentimentData (mirrors what the producer sends)
            SentimentData sentimentData = objectMapper.readValue(message, SentimentData.class);

            log.info("Received weekly-sentiment event for email={}", sentimentData.getEmail());

            emailService.sendEmail(
                    sentimentData.getEmail(),
                    EMAIL_SUBJECT,
                    sentimentData.getSentiment()
            );

            log.info("Sent weekly-sentiment email to {}", sentimentData.getEmail());

        } catch (Exception e) {
            // Do NOT rethrow — prevents Kafka from redelivering an unprocessable message forever.
            log.error("Failed to process weekly-sentiment message: {} | error: {}",
                    message, e.getMessage(), e);
        }
    }
}