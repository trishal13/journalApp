package com.trishal.journalApp.exception;

public class UserNotFoundException extends JournalAppException {
    public UserNotFoundException(String username) {
        super(ErrorCode.USER_NOT_FOUND, "username: " + username);
    }
}