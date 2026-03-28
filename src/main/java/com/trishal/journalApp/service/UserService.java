package com.trishal.journalApp.service;

import com.trishal.journalApp.dto.UserUpdateRequestDto;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.exception.UserNotFoundException;
import com.trishal.journalApp.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    private UserRepo userRepo;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ── Create ────────────────────────────────────────────────────────────────

    public void saveNewUser(User user) {
        if (userRepo.existsByUserName(user.getUserName())) {
            throw new JournalAppException(ErrorCode.USER_ALREADY_EXISTS,
                    "username: " + user.getUserName());
        }
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
        if (userRepo.existsByUserName(user.getUserName())) {
            throw new JournalAppException(ErrorCode.USER_ALREADY_EXISTS,
                    "username: " + user.getUserName());
        }
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER", "ADMIN"));
            userRepo.save(user);
        } catch (Exception e) {
            log.error("Failed to save admin user username={}", user.getUserName(), e);
            throw new JournalAppException(ErrorCode.USER_CREATION_FAILED, e);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Safely update only the fields present in the request DTO.
     *
     * Rules:
     *  - userName         : updated only if non-blank AND different from current.
     *                       Uniqueness is checked before changing.
     *  - password         : updated only if non-blank; encoded here, never elsewhere.
     *  - email            : updated only if non-blank.
     *  - sentimentAnalysis: updated only if explicitly provided (non-null).
     *  - roles / entries  : NEVER touched — managed by dedicated flows only.
     *
     * Bug fixed: old controller called saveNewUser() which unconditionally
     * re-encoded the already-hashed password and reset roles to ["USER"],
     * breaking login and stripping admin rights on every profile update.
     */
    public User updateUser(User existingUser, UserUpdateRequestDto dto) {
        try {
            if (StringUtils.isNotBlank(dto.getUserName())
                    && !dto.getUserName().equals(existingUser.getUserName())) {
                if (userRepo.existsByUserName(dto.getUserName())) {
                    throw new JournalAppException(ErrorCode.USER_ALREADY_EXISTS,
                            "username: " + dto.getUserName());
                }
                existingUser.setUserName(dto.getUserName());
            }

            if (StringUtils.isNotBlank(dto.getPassword())) {
                existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            if (StringUtils.isNotBlank(dto.getEmail())) {
                existingUser.setEmail(dto.getEmail());
            }

            if (!ObjectUtils.isEmpty(dto.getSentimentAnalysis())) {
                existingUser.setSentimentAnalysis(dto.getSentimentAnalysis());
            }

            userRepo.save(existingUser);
            log.info("Updated user id={}", existingUser.getUserId());
            return existingUser;

        } catch (JournalAppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update user id={}", existingUser.getUserId(), e);
            throw new JournalAppException(ErrorCode.USER_UPDATE_FAILED, e);
        }
    }

    /**
     * Persist a user entity as-is — no duplicate check, no password encoding.
     * Used internally (e.g. after removing a journal entry from the user's list).
     */
    public void saveEntry(User user) {
        try {
            userRepo.save(user);
        } catch (Exception e) {
            log.error("Failed to save user id={}", user.getUserId(), e);
            throw new JournalAppException(ErrorCode.USER_UPDATE_FAILED, e);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<User> getAll() {
        return userRepo.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        return userRepo.findById(id);
    }

    public User findByUserName(String userName) {
        User user = userRepo.findByUserName(userName);
        if (ObjectUtils.isEmpty(user)) {
            throw new UserNotFoundException(userName);
        }
        return user;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteUserById(UUID id) {
        userRepo.deleteById(id);
    }

    /**
     * BUG FIX: UserRepo.deleteByUserName() now returns void (not User).
     * Spring Data derived deletes return int (row count) — casting to User
     * threw ClassCastException at runtime.
     *
     * The controller only needs confirmation that the delete succeeded, so
     * void is the correct return type here too.
     */
    public void deleteByUserName(String userName) {
        try {
            userRepo.deleteByUserName(userName);
            log.info("Deleted user username={}", userName);
        } catch (Exception e) {
            log.error("Failed to delete user username={}", userName, e);
            throw new JournalAppException(ErrorCode.USER_DELETION_FAILED, e);
        }
    }
}