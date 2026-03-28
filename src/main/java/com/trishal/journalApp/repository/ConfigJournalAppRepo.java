package com.trishal.journalApp.repository;

import com.trishal.journalApp.entity.ConfigJournalApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigJournalAppRepo extends JpaRepository<ConfigJournalApp, UUID> {

    /**
     * Lookup a config entry by its key (e.g. "WEATHER_API", "GEMINI_API").
     * Returns Optional.empty() if the key doesn't exist, letting the service
     * throw a meaningful error instead of a NullPointerException.
     */
    Optional<ConfigJournalApp> findByKey(String key);
}