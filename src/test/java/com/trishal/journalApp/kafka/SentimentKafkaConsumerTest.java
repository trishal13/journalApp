package com.trishal.journalApp.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trishal.journalApp.kafka.consumer.SentimentKafkaConsumer;
import com.trishal.journalApp.model.SentimentData;
import com.trishal.journalApp.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentimentKafkaConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SentimentKafkaConsumer consumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void consume_shouldDeserializeAndSendEmail() throws Exception {
        SentimentData data = SentimentData.builder()
                .email("user@example.com")
                .sentiment("Your weekly report: HAPPY")
                .build();
        String json = objectMapper.writeValueAsString(data);

        consumer.consume(json);

        verify(emailService).sendEmail(
                "user@example.com",
                "Your Weekly Sentiment Report",
                "Your weekly report: HAPPY"
        );
    }

    @Test
    void consume_shouldNotThrow_whenMessageIsInvalid() {
        // Should log error but not rethrow
        assertThatCode(() -> consumer.consume("invalid-json"))
                .doesNotThrowAnyException();

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void consume_shouldNotThrow_whenEmailServiceFails() throws Exception {
        SentimentData data = SentimentData.builder()
                .email("user@example.com")
                .sentiment("report")
                .build();
        String json = objectMapper.writeValueAsString(data);

        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendEmail(anyString(), anyString(), anyString());

        // Consumer catches all exceptions to prevent Kafka redelivery loops
        assertThatCode(() -> consumer.consume(json)).doesNotThrowAnyException();
    }
}
