package com.trishal.journalApp.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.model.SentimentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Single place for all Kafka publish operations.
 *
 * Uses KafkaTemplate<String, String> to match the StringSerializer configured in
 * application.properties. SentimentData is serialised to a JSON string before sending.
 */
@Slf4j
@Component
public class SentimentKafkaProducer {

    // FIX 1: was "{weekly.sentiment.topic}" — missing $ prefix
    @Value("${weekly.sentiment.topic}")
    private String weeklySentimentTopic;

    // KafkaTemplate type matches spring.kafka.producer.value-serializer=StringSerializer
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Publish a {@link SentimentData} event to the weekly-sentiments topic.
     * The user's email is the Kafka partition key (ordering guarantee per user).
     *
     * @throws JournalAppException (ERR_4004) if the send fails synchronously.
     */
    public void sendWeeklySentiment(SentimentData sentimentData) {
        try {
            // Serialise to JSON string — aligns with StringSerializer in properties
            String payload = objectMapper.writeValueAsString(sentimentData);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(weeklySentimentTopic, sentimentData.getEmail(), payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to deliver weekly-sentiment for email={}: {}",
                            sentimentData.getEmail(), ex.getMessage(), ex);
                } else {
                    log.debug("Published weekly-sentiment for email={} → partition={} offset={}",
                            sentimentData.getEmail(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Kafka send threw synchronously for email={}: {}",
                    sentimentData.getEmail(), e.getMessage(), e);
            throw new JournalAppException(ErrorCode.KAFKA_PUBLISH_FAILED, e);
        }
    }
}