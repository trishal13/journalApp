package com.trishal.journalApp.service;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.constants.Placeholders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppCache appCache;

    public WeatherResponse getWeather(String city){
        String apiUrl = appCache.appCache.get(AppCache.keys.WEATHER_API.toString()).replace(Placeholders.CITY, city).replace(Placeholders.API_KEY, apiKey);
        ResponseEntity<WeatherResponse> response = restTemplate.exchange(apiUrl, HttpMethod.GET, null, WeatherResponse.class);
        WeatherResponse responseBody = response.getBody();
        return responseBody;
    }
}
