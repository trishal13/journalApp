package com.trishal.journalApp.controller;

import com.trishal.journalApp.api.response.WeatherResponse;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private WeatherService weatherService;

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User newUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User oldUser = userService.findByUserName(userName); // throws UserNotFoundException if missing

        if (newUser.getUserName() != null && !newUser.getUserName().isEmpty()) {
            oldUser.setUserName(newUser.getUserName());
        }
        // BUG FIX: was setting oldUser.getPassword() (no change) instead of newUser.getPassword()
        if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
            oldUser.setPassword(newUser.getPassword());
        }
        userService.saveNewUser(oldUser); // re-encodes password
        return new ResponseEntity<>(oldUser, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        userService.deleteByUserName(userName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<String> greetings(@RequestParam String city) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        WeatherResponse weatherResponse = weatherService.getWeather(city);
        StringBuilder message = new StringBuilder("Hi " + userName);
        if (!Objects.isNull(weatherResponse)) {
            message.append(", Weather feels like: ").append(weatherResponse.getCurrent().getFeelslike()).append("°C");
        }
        return new ResponseEntity<>(message.toString(), HttpStatus.OK);
    }
}