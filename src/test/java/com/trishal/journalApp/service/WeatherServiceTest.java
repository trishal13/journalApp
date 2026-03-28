package com.trishal.journalApp.service;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.exception.JournalAppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private AppCache appCache;
    @Mock private RedisService redisService;

    @InjectMocks
    private WeatherService weatherService;

    private WeatherResponse weatherResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weatherService, "apiKey", "test-key");
        weatherResponse = WeatherResponse.builder()
                .main(WeatherResponse.Main.builder().temp(300.0).feelsLike(302.0).humidity(60).build())
                .build();
        Map<String, String> cacheMap = new HashMap<>();
        cacheMap.put("WEATHER_API", "https://api.openweathermap.org/data/2.5/weather?lat=<lat>&lon=<lon>&appid=<apiKey>");
        appCache.appCache = cacheMap;
    }

    @Test
    void getWeather_shouldReturnCachedResponse_whenCacheHit() {
        when(redisService.get("weather_of_28.6_77.2", WeatherResponse.class)).thenReturn(weatherResponse);
        WeatherResponse result = weatherService.getWeather(28.6, 77.2);
        assertThat(result).isEqualTo(weatherResponse);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(WeatherResponse.class));
    }

    @Test
    void getWeather_shouldCallApiAndCache_whenCacheMiss() {
        when(redisService.get("weather_of_28.6_77.2", WeatherResponse.class)).thenReturn(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(WeatherResponse.class)))
                .thenReturn(new ResponseEntity<>(weatherResponse, HttpStatus.OK));

        WeatherResponse result = weatherService.getWeather(28.6, 77.2);

        assertThat(result).isEqualTo(weatherResponse);
        verify(redisService).set(eq("weather_of_28.6_77.2"), eq(weatherResponse), eq(300L));
    }

    @Test
    void getWeather_shouldThrow_whenApiFails() {
        when(redisService.get(anyString(), eq(WeatherResponse.class))).thenReturn(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(WeatherResponse.class)))
                .thenThrow(new RuntimeException("API timeout"));
        assertThatThrownBy(() -> weatherService.getWeather(28.6, 77.2)).isInstanceOf(JournalAppException.class);
    }
}