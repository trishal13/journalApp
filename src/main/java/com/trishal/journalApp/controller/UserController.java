package com.trishal.journalApp.controller;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.dto.ApiResponse;
import com.trishal.journalApp.dto.UserUpdateRequestDto;
import com.trishal.journalApp.dto.UserUpdateResponseDto;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.mapper.UserMapper;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeatherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private UserMapper userMapper;

    @PutMapping
    public ResponseEntity<ApiResponse<UserUpdateResponseDto>> updateUser(
            @Valid @RequestBody UserUpdateRequestDto dto) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User existingUser = userService.findByUserName(authentication.getName());
        User updated = userService.updateUser(existingUser, dto);
        return ResponseEntity.ok(
                ApiResponse.success(userMapper.toUpdateResponse(updated), "User updated successfully."));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        userService.deleteByUserName(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully."));
    }

    /**
     * Returns a personalised greeting with live weather for the given coordinates.
     *
     * Changed from ?city=X to ?lat=X&lon=Y to match the new OpenWeatherMap API.
     * Temperatures are converted from Kelvin to Celsius before display.
     *
     * Example: GET /user?lat=28.6139&lon=77.2090
     */
    @GetMapping
    public ResponseEntity<ApiResponse<String>> greetings(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        WeatherResponse weatherResponse = weatherService.getWeather(lat, lon);

        StringBuilder message = new StringBuilder("Hi " + userName);
        if (!ObjectUtils.isEmpty(weatherResponse) && !ObjectUtils.isEmpty(weatherResponse.getMain())) {
            message.append(", Weather: ")
                    .append(weatherResponse.getWeatherDescription())
                    .append(", Feels like: ")
                    .append(weatherResponse.getMain().getFeelsLikeCelsius())
                    .append("°C");
        }

        return ResponseEntity.ok(ApiResponse.success(message.toString(), "Greeting retrieved."));
    }
}