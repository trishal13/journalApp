package com.trishal.journalApp.service;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.constants.Placeholders;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Service
public class WeatherService {

    private static final String CACHE_KEY_PREFIX = "weather_of_";
    private static final long CACHE_TTL_SECONDS  = 300L; // 5 minutes

    @Value("${weather.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppCache appCache;

    @Autowired
    private RedisService redisService;

    /**
     * Returns weather data for the given city.
     * Checks Redis cache first; on miss fetches from the external API and caches the result.
     *
     * @throws JournalAppException (ERR_4001) if the external call fails
     */
    public WeatherResponse getWeather(String city) {
        String cacheKey = CACHE_KEY_PREFIX + city.toLowerCase();

        // 1. Try cache
        WeatherResponse cached = redisService.get(cacheKey, WeatherResponse.class);
        if (!Objects.isNull(cached)) {
            log.debug("Weather cache HIT for city={}", city);
            return cached;
        }

        // 2. Cache miss — call external API
        try {
            String apiUrl = appCache.appCache
                    .get(AppCache.keys.WEATHER_API.toString())
                    .replace(Placeholders.CITY,    city)
                    .replace(Placeholders.API_KEY, apiKey);

            ResponseEntity<WeatherResponse> responseEntity =
                    restTemplate.exchange(apiUrl, HttpMethod.GET, null, WeatherResponse.class);

            WeatherResponse body = responseEntity.getBody();
            if (!Objects.isNull(body)) {
                redisService.set(cacheKey, body, CACHE_TTL_SECONDS);
                log.debug("Weather cache SET for city={}", city);
            }
            return body;

        } catch (Exception e) {
            log.error("Weather API call failed for city={}: {}", city, e.getMessage(), e);
            throw new JournalAppException(ErrorCode.WEATHER_SERVICE_UNAVAILABLE, e);
        }
    }
}