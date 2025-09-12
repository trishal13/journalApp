package com.trishal.journalApp.controller;

import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private UserService userService;

    @GetMapping("/health-check")
    public String healthCheck(){
        return "Ok";
    }

    @PostMapping("/create-user")
    public ResponseEntity<User> createUser(@RequestBody User newUser){
        try{
            userService.saveNewUser(newUser);
            return new ResponseEntity<>(newUser, HttpStatus.CREATED);
        }
        catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
