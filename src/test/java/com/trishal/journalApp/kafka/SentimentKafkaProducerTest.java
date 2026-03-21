package com.trishal.journalApp.kafka;

import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.kafka.producer.SentimentKafkaProducer;
import com.trishal.journalApp.model.SentimentData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentimentKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private SentimentKafkaProducer producer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "weeklySentimentTopic", "weekly-sentiments");
    }

    @Test
    void sendWeeklySentiment_shouldPublishToKafka() {
        SentimentData data = SentimentData.builder()
                .email("user@example.com")
                .sentiment("HAPPY report")
                .build();

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(eq("weekly-sentiments"), eq("user@example.com"), anyString()))
                .thenReturn(future);

        assertThatCode(() -> producer.sendWeeklySentiment(data)).doesNotThrowAnyException();
        verify(kafkaTemplate).send(eq("weekly-sentiments"), eq("user@example.com"), anyString());
    }

    @Test
    void sendWeeklySentiment_shouldThrowJournalAppException_whenSerializationFails() {
        // Pass null data to trigger serialization error
        SentimentData data = SentimentData.builder()
                .email("user@example.com")
                .sentiment("report")
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        assertThatThrownBy(() -> producer.sendWeeklySentiment(data))
                .isInstanceOf(JournalAppException.class);
    }
}
