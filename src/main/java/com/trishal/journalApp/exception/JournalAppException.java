package com.trishal.journalApp.exception;

import lombok.Getter;

@Getter
public class JournalAppException extends RuntimeException {

    private final ErrorCode errorCode;

    public JournalAppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public JournalAppException(ErrorCode errorCode, String extraDetail) {
        super(errorCode.getMessage() + " | " + extraDetail);
        this.errorCode = errorCode;
    }

    public JournalAppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}