package com.trishal.journalApp.service;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.dto.ConfigUpdateRequestDto;
import com.trishal.journalApp.entity.ConfigJournalApp;
import com.trishal.journalApp.exception.ErrorCode;
import com.trishal.journalApp.exception.JournalAppException;
import com.trishal.journalApp.repository.ConfigJournalAppRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ConfigService {

    @Autowired
    private ConfigJournalAppRepo configJournalAppRepo;

    @Autowired
    private AppCache appCache;

    /**
     * Returns all config entries — used by the admin list endpoint.
     */
    public List<ConfigJournalApp> getAllConfigs() {
        return configJournalAppRepo.findAll();
    }

    /**
     * Updates the value for the given key.
     *
     * Throws INTERNAL_SERVER_ERROR (reusing the closest generic code) if the
     * key doesn't exist — admins should not be able to silently create new
     * config entries through this endpoint; use a DB migration for that.
     *
     * After persisting, AppCache is refreshed immediately so the new value
     * is picked up by WeatherService / GeminiService without waiting for the
     * 10-minute scheduler tick.
     */
    public ConfigJournalApp updateConfig(String key, ConfigUpdateRequestDto dto) {
        ConfigJournalApp config = configJournalAppRepo.findByKey(key)
                .orElseThrow(() -> new JournalAppException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Config key not found: " + key));

        config.setValue(dto.getValue());

        try {
            configJournalAppRepo.save(config);
            log.info("Config key='{}' updated.", key);
        } catch (Exception e) {
            log.error("Failed to update config key='{}'", key, e);
            throw new JournalAppException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }

        // Refresh in-memory cache immediately so the change takes effect now.
        appCache.init();
        log.info("AppCache refreshed after config update for key='{}'.", key);

        return config;
    }
}