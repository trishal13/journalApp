package com.trishal.journalApp.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for the Weatherstack API.
 *
 * All inner classes carry @Builder + @NoArgsConstructor + @AllArgsConstructor:
 *  - @Builder       → lets us construct instances cleanly in tests and service code
 *  - @NoArgsConstructor → required by Jackson for JSON deserialization
 *  - @AllArgsConstructor → required by Lombok @Builder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private Current current;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current {

        private int temperature;

        @JsonProperty("weather_descriptions")
        private List<String> weatherDescriptions;

        private int feelslike;

        @JsonProperty("wind_speed")
        private int windSpeed;

        private int humidity;
    }
}