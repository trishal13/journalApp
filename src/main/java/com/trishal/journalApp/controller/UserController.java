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
    public ResponseEntity<User> updateUser(@RequestBody User newUser){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User oldUser = userService.findByUserName(userName);
        if (Objects.isNull(oldUser)){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        if (!newUser.getUserName().isEmpty()){
            oldUser.setUserName(newUser.getUserName());
        }
        if (!newUser.getPassword().isEmpty()){
            oldUser.setPassword(oldUser.getPassword());
        }
        userService.saveNewUser(oldUser);
        return new ResponseEntity<>(oldUser, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<User> deleteUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.deleteByUserName(userName);
        return new ResponseEntity<>(user, HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<?> greetings(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        WeatherResponse weatherResponse = weatherService.getWeather("Indore");
        StringBuilder message = new StringBuilder("Hi " + userName);
        if (!Objects.isNull(weatherResponse)){
            message.append(", Weather feelsLike: " + weatherResponse.getCurrent().getFeelslike());
        }
        return new ResponseEntity<>(message.toString(), HttpStatus.OK);
    }


}
