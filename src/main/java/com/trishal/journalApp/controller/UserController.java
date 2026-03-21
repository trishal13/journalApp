package com.trishal.journalApp.controller;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.dto.ApiResponse;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeatherService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @PutMapping
    public ResponseEntity<ApiResponse<User>> updateUser(@RequestBody User newUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User oldUser = userService.findByUserName(userName);

        if (StringUtils.isNotEmpty(newUser.getUserName())) {
            oldUser.setUserName(newUser.getUserName());
        }
        if (StringUtils.isNotEmpty(newUser.getPassword())) {
            oldUser.setPassword(newUser.getPassword());
        }
        userService.saveNewUser(oldUser);
        return ResponseEntity.ok(ApiResponse.success(oldUser, "User updated successfully."));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        userService.deleteByUserName(userName);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<String>> greetings(@RequestParam String city) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        WeatherResponse weatherResponse = weatherService.getWeather(city);
        StringBuilder message = new StringBuilder("Hi " + userName);
        if (!ObjectUtils.isEmpty(weatherResponse)) {
            message.append(", Weather feels like: ").append(weatherResponse.getCurrent().getFeelslike()).append("°C");
        }
        return ResponseEntity.ok(ApiResponse.success(message.toString(), "Greeting retrieved."));
    }
}
