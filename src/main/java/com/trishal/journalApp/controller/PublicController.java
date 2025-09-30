package com.trishal.journalApp.controller;

import com.trishal.journalApp.dto.UserLoginRequestDto;
import com.trishal.journalApp.dto.UserLoginResponseDto;
import com.trishal.journalApp.dto.UserRegistrationRequestDto;
import com.trishal.journalApp.dto.UserResponseDto;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.mapper.UserMapper;
import com.trishal.journalApp.service.UserService;
import com.trishal.journalApp.service.impl.UserDetailServiceImpl;
import com.trishal.journalApp.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    public String healthCheck(){
        return "Ok";
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@RequestBody UserRegistrationRequestDto newUserRequest){
        try{
            User newUser = userMapper.toEntity(newUserRequest);
            userService.saveNewUser(newUser);
            UserResponseDto userResponseDto = userMapper.toResponse(newUser);
            return new ResponseEntity<>(userResponseDto, HttpStatus.CREATED);
        }
        catch (Exception e){
            log.error("Error during new user signup!");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto userLoginRequestDto){
        try{
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginRequestDto.getUserName(), userLoginRequestDto.getPassword()));
            UserDetails userDetails = userDetailService.loadUserByUsername(userLoginRequestDto.getUserName());
            String jwtToken = jwtUtil.generateToken(userDetails.getUsername());
            User user = userService.findByUserName(userLoginRequestDto.getUserName());
            UserLoginResponseDto userLoginResponseDto = UserLoginResponseDto.builder()
                    .token(jwtToken)
                    .tokenType("Bearer")
                    .userName(user.getUserName())
                    .roles(user.getRoles())
                    .expiresIn(3600)
                    .build();
            return new ResponseEntity<>(userLoginResponseDto, HttpStatus.OK);
        }
        catch (Exception e){
            log.error("Exception occured while user login ", e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

}
