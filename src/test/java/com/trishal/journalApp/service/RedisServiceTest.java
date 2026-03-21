package com.trishal.journalApp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @Test
    void get_shouldReturnDeserialisedObject_whenKeyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test-key")).thenReturn("{\"email\":\"a@b.com\",\"sentiment\":\"HAPPY\"}");

        var result = redisService.get("test-key", com.trishal.journalApp.model.SentimentData.class);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void get_shouldReturnNull_whenKeyDoesNotExist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("missing-key")).thenReturn(null);

        var result = redisService.get("missing-key", String.class);

        assertThat(result).isNull();
    }

    @Test
    void get_shouldReturnNull_whenDeserializationFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("bad-key")).thenReturn("not-valid-json");

        var result = redisService.get("bad-key", com.trishal.journalApp.model.SentimentData.class);

        assertThat(result).isNull();
    }

    @Test
    void set_shouldSerializeAndStoreWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.set("key", "value", 60L);

        verify(valueOperations).set(eq("key"), anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void delete_shouldDelegateToRedisTemplate() {
        redisService.delete("key");

        verify(redisTemplate).delete("key");
    }

    @Test
    void set_shouldNotThrow_whenRedisFails() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        // Should not throw — errors are logged and swallowed
        assertThatCode(() -> redisService.set("key", "value", 60L)).doesNotThrowAnyException();
    }

    @Test
    void delete_shouldNotThrow_whenRedisFails() {
        when(redisTemplate.delete("key")).thenThrow(new RuntimeException("Redis down"));

        assertThatCode(() -> redisService.delete("key")).doesNotThrowAnyException();
    }
}
