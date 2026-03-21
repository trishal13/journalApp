package com.trishal.journalApp.cache;

import com.trishal.journalApp.entity.ConfigJournalApp;
import com.trishal.journalApp.repository.ConfigJournalAppRepo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AppCache {

    public enum keys {
        WEATHER_API
    }

    @Autowired
    private ConfigJournalAppRepo configJournalAppRepo;

    public Map<String, String> appCache;

    @PostConstruct
    public void init() {
        appCache = new HashMap<>();
        List<ConfigJournalApp> all = configJournalAppRepo.findAll();
        all.forEach(config -> appCache.put(config.getKey(), config.getValue()));
        log.info("AppCache loaded with {} entries.", appCache.size());
    }
}