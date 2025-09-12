package com.trishal.journalApp.controller;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppCache appCache;

    @GetMapping("/all-users")
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> allUsers = userService.getAll();
        if (!Objects.isNull(allUsers) && !allUsers.isEmpty()){
            return new ResponseEntity<>(allUsers, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/create-admin-user")
    public ResponseEntity<User> createAdmin(@RequestBody User user){
        userService.saveAdmin(user);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/clear-app-cache")
    public void clearAppCache(){
        appCache.init();
    }
}
