package com.trishal.journalApp.service;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.constants.Placeholders;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class WeatherService {

    private static final String CACHE_KEY_PREFIX = "weather_of_";
    private static final long   CACHE_TTL_SECONDS = 300L; // 5 minutes

    @Value("${weather.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppCache appCache;

    @Autowired
    private RedisService redisService;

    /**
     * Returns weather data for the given coordinates.
     *
     * Cache key is "weather_of_{lat}_{lon}" — specific enough that
     * two nearby but distinct locations don't share a cache entry.
     *
     * Temperature values in WeatherResponse are Kelvin; use
     * getTempCelsius() / getFeelsLikeCelsius() helpers when displaying.
     *
     * @param lat latitude
     * @param lon longitude
     * @throws JournalAppException (ERR_4001) if the external call fails
     */
    public WeatherResponse getWeather(double lat, double lon) {
        String cacheKey = CACHE_KEY_PREFIX + lat + "_" + lon;

        // 1. Try cache
        WeatherResponse cached = redisService.get(cacheKey, WeatherResponse.class);
        if (!ObjectUtils.isEmpty(cached)) {
            log.debug("Weather cache HIT for lat={} lon={}", lat, lon);
            return cached;
        }

        // 2. Cache miss — call OpenWeatherMap
        try {
            String apiUrl = appCache.appCache
                    .get(AppCache.keys.WEATHER_API.toString())
                    .replace(Placeholders.LAT,     String.valueOf(lat))
                    .replace(Placeholders.LON,     String.valueOf(lon))
                    .replace(Placeholders.API_KEY, apiKey);

            ResponseEntity<WeatherResponse> responseEntity =
                    restTemplate.exchange(apiUrl, HttpMethod.GET, null, WeatherResponse.class);

            WeatherResponse body = responseEntity.getBody();
            if (!ObjectUtils.isEmpty(body)) {
                redisService.set(cacheKey, body, CACHE_TTL_SECONDS);
                log.debug("Weather cache SET for lat={} lon={}", lat, lon);
            }
            return body;

        } catch (Exception e) {
            log.error("Weather API call failed for lat={} lon={}: {}", lat, lon, e.getMessage(), e);
            throw new JournalAppException(ErrorCode.WEATHER_SERVICE_UNAVAILABLE, e);
        }
    }
}