package com.trishal.journalApp.service;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.constants.Placeholders;
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

    @Value("${weather.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppCache appCache;

    @Autowired
    private RedisService redisService;

    public WeatherResponse getWeather(String city){
        try{
            WeatherResponse weatherResponse = redisService.get("weather_of_" + city, WeatherResponse.class);
            if (!Objects.isNull(weatherResponse)){
                return weatherResponse;
            }
            String apiUrl = appCache.appCache.get(AppCache.keys.WEATHER_API.toString()).replace(Placeholders.CITY, city).replace(Placeholders.API_KEY, apiKey);
            ResponseEntity<WeatherResponse> response = restTemplate.exchange(apiUrl, HttpMethod.GET, null, WeatherResponse.class);
            WeatherResponse responseBody = response.getBody();
            if (!Objects.isNull(responseBody)){
                redisService.set("weather_of_" + city, responseBody, 300L);
            }
            return responseBody;
        }
        catch (Exception e){
            log.error("Exception occured at get weather ", e);
        }
        return null;
    }
}
