package com.trishal.journalApp.cache;

import com.trishal.journalApp.entity.ConfigJournalApp;
import com.trishal.journalApp.repository.ConfigJournalAppRepo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AppCache {

    public enum keys{
        WEATHER_API
    }
    @Autowired
    private ConfigJournalAppRepo configJournalAppRepo;

    public Map<String, String> appCache;

    @PostConstruct
    public void init(){
        appCache = new HashMap<>();
        List<ConfigJournalApp> all = configJournalAppRepo.findAll();
        all.forEach(configJournalApp ->
                appCache.put(configJournalApp.getKey(),configJournalApp.getValue())
        );
    }

}
