package com.trishal.journalApp.repository;

import com.trishal.journalApp.entity.ConfigJournalApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConfigJournalAppRepo extends JpaRepository<ConfigJournalApp, UUID> {

}