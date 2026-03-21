package com.trishal.journalApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka configuration.
 *
 * Spring Boot auto-configures ProducerFactory, ConsumerFactory, and KafkaTemplate
 * from the spring.kafka.* properties in application.properties (including SASL_SSL
 * for Redpanda and plain-text override for local dev in application-local.properties).
 *
 * We only define the KafkaListenerContainerFactory here so we can attach a custom
 * error handler with a retry + backoff policy.
 *
 * DO NOT redefine ProducerFactory or ConsumerFactory beans here — that would
 * override Boot's auto-configuration and ignore the SASL settings in properties.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    /**
     * Listener container factory used by @KafkaListener in SentimentKafkaConsumer.
     *
     * Error handling: retry up to 3 times with a 2-second gap before giving up.
     * After all retries are exhausted the record is logged and skipped (no requeue),
     * preventing poison-pill messages from blocking the consumer indefinitely.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 3 retries, 2 000 ms apart — then log & skip the bad record
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(2000L, 3L))
        );

        return factory;
    }
}