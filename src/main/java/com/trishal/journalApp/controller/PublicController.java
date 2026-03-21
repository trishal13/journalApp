package com.trishal.journalApp.controller;

import com.trishal.journalApp.dto.*;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.mapper.UserMapper;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.impl.UserDetailServiceImpl;
import com.trishal.journalApp.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailServiceImpl userDetailService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/health-check")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Service is healthy."));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponseDto>> signup(
            @Valid @RequestBody UserRegistrationRequestDto newUserRequest) {

        User newUser = userMapper.toEntity(newUserRequest);
        userService.saveNewUser(newUser);
        return new ResponseEntity<>(
                ApiResponse.success(userMapper.toResponse(newUser), "User registered successfully."),
                HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponseDto>> login(
            @Valid @RequestBody UserLoginRequestDto userLoginRequestDto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userLoginRequestDto.getUserName(),
                        userLoginRequestDto.getPassword()
                )
        );

        UserDetails userDetails = userDetailService.loadUserByUsername(userLoginRequestDto.getUserName());
        String jwtToken = jwtUtil.generateToken(userDetails.getUsername());
        User user = userService.findByUserName(userLoginRequestDto.getUserName());

        UserLoginResponseDto loginResponse = UserLoginResponseDto.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .userName(user.getUserName())
                .roles(user.getRoles())
                .expiresIn(3600)
                .build();

        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Login successful."));
    }
}
