package com.trishal.journalApp.exception;

import java.util.UUID;

public class JournalEntryAccessDeniedException extends JournalAppException {
    public JournalEntryAccessDeniedException(UUID id) {
        super(ErrorCode.JOURNAL_ENTRY_ACCESS_DENIED, "id: " + id);
    }
}