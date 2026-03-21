package com.trishal.journalApp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retrieve a value from Redis and deserialise it into {@code entityClass}.
     *
     * @return the deserialised object, or {@code null} if the key does not exist
     */
    public <T> T get(String key, Class<T> entityClass) {
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (Objects.isNull(raw)) {
                return null;
            }
            return objectMapper.readValue(raw.toString(), entityClass);
        } catch (Exception e) {
            log.error("Redis GET failed for key='{}': {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Serialise {@code value} to JSON and store it in Redis with a TTL.
     *
     * @param key  cache key
     * @param value object to cache
     * @param ttl  time-to-live in seconds
     */
    public void set(String key, Object value, Long ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis SET failed for key='{}': {}", key, e.getMessage(), e);
        }
    }

    /**
     * Delete a key from Redis.
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis DELETE failed for key='{}': {}", key, e.getMessage(), e);
        }
    }
}