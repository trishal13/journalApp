package com.trishal.journalApp.service;

import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepo userRepo;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void saveNewUser(User user){
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER"));
            userRepo.save(user);
        }
        catch (Exception e){
            logger.error("Exception: ", e); // from logger instance
            log.error("Exception: ", e);    // from @Slf4j
            throw new RuntimeException("An error occured!");
        }
    }

    public void saveAdmin(User user){
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER", "ADMIN"));
            userRepo.save(user);
        }
        catch (Exception e){
            logger.error("Exception: ", e);
            throw new RuntimeException("An error occured!");
        }
    }

    public void saveEntry(User user){
        try {
            userRepo.save(user);
        }
        catch (Exception e){
            logger.error("Exception: ", e);
            throw new RuntimeException("An error occured!");
        }
    }

    public List<User> getAll(){
        return userRepo.findAll();
    }

    public Optional<User> getUserById(UUID id){
        return userRepo.findById(id);
    }

    public void deleteUserById(UUID id){
        userRepo.deleteById(id);
    }

    public User findByUserName(String userName){
        return userRepo.findByUserName(userName);
    }

    public User deleteByUserName(String userName){
        return userRepo.deleteByUserName(userName);
    }
}
