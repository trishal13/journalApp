package com.trishal.journalApp.service;

import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.exception.UserNotFoundException;
import com.trishal.journalApp.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepo userRepo;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ── Create / Update ───────────────────────────────────────────────────────

    public void saveNewUser(User user) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER"));
            userRepo.save(user);
        } catch (Exception e) {
            log.error("Failed to save new user username={}", user.getUserName(), e);
            throw new JournalAppException(ErrorCode.USER_CREATION_FAILED, e);
        }
    }

    public void saveAdmin(User user) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER", "ADMIN"));
            userRepo.save(user);
        } catch (Exception e) {
            log.error("Failed to save admin user username={}", user.getUserName(), e);
            throw new JournalAppException(ErrorCode.USER_CREATION_FAILED, e);
        }
    }

    /** Persist a user without re-encoding the password (used for internal updates). */
    public void saveEntry(User user) {
        try {
            userRepo.save(user);
        } catch (Exception e) {
            log.error("Failed to save user id={}", user.getUserId(), e);
            throw new JournalAppException(ErrorCode.USER_UPDATE_FAILED, e);
        }
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public List<User> getAll() {
        return userRepo.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        return userRepo.findById(id);
    }

    public User findByUserName(String userName) {
        User user = userRepo.findByUserName(userName);
        if (Objects.isNull(user)) {
            throw new UserNotFoundException(userName);
        }
        return user;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteUserById(UUID id) {
        userRepo.deleteById(id);
    }

    public User deleteByUserName(String userName) {
        try {
            return userRepo.deleteByUserName(userName);
        } catch (Exception e) {
            log.error("Failed to delete user username={}", userName, e);
            throw new JournalAppException(ErrorCode.USER_DELETION_FAILED, e);
        }
    }
}