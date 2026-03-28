package com.trishal.journalApp.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for the OpenWeatherMap /data/2.5/weather endpoint.
 *
 * Only the fields actually used by the application are mapped.
 * All unknown fields are silently ignored via @JsonIgnoreProperties.
 *
 * Temperature values from OpenWeatherMap are in Kelvin by default.
 * The Main class provides a helper to convert to Celsius.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private Main main;
    private List<Weather> weather;
    private Wind wind;
    private String name;     // city / area name e.g. "Province of Turin"
    private int visibility;  // metres
    private int cod;         // HTTP-style status code from OWM (200 = ok)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {

        private double temp;

        @JsonProperty("feels_like")
        private double feelsLike;

        @JsonProperty("temp_min")
        private double tempMin;

        @JsonProperty("temp_max")
        private double tempMax;

        private int pressure;
        private int humidity;

        /** Converts Kelvin → Celsius, rounded to 1 decimal place. */
        public double getTempCelsius() {
            return Math.round((temp - 273.15) * 10.0) / 10.0;
        }

        /** Converts feels_like Kelvin → Celsius, rounded to 1 decimal place. */
        public double getFeelsLikeCelsius() {
            return Math.round((feelsLike - 273.15) * 10.0) / 10.0;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        private int id;
        private String main;         // e.g. "Rain"
        private String description;  // e.g. "moderate rain"
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        private double speed;
        private int deg;
        private double gust;
    }

    /** Convenience: description from the first weather entry, or empty string. */
    public String getWeatherDescription() {
        if (weather != null && !weather.isEmpty()) {
            return weather.get(0).getDescription();
        }
        return "";
    }
}