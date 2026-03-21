package com.trishal.journalApp.exception;

import java.util.UUID;

public class JournalEntryNotFoundException extends JournalAppException {
    public JournalEntryNotFoundException(UUID id) {
        super(ErrorCode.JOURNAL_ENTRY_NOT_FOUND, "id: " + id);
    }
}