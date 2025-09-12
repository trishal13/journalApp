package com.trishal.journalApp.repository;

import com.trishal.journalApp.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalEntryRepo extends JpaRepository<JournalEntry, UUID> {

}
