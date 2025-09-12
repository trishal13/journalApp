package com.trishal.journalApp.repository;

import com.trishal.journalApp.entity.ConfigJournalApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfigJournalAppRepo extends JpaRepository<ConfigJournalApp, UUID> {

}
