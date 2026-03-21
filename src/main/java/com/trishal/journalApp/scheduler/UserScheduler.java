package com.trishal.journalApp.scheduler;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.service.WeeklySentimentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserScheduler {

    @Autowired
    private WeeklySentimentService weeklySentimentService;

    @Autowired
    private AppCache appCache;

    /**
     * Every Sunday at 09:00 — send weekly sentiment report emails.
     * All logic is in {@link WeeklySentimentService#runWeeklySentimentReport()}.
     */
    @Scheduled(cron = "0 0 9 * * SUN")
    public void fetchUsersAndSendSAMail() {
        log.info("Scheduled weekly sentiment report triggered.");
        weeklySentimentService.runWeeklySentimentReport();
    }

    /**
     * Refresh app-config cache every 10 minutes.
     */
    @Scheduled(cron = "0 0/10 * ? * *")
    public void clearAppCache() {
        log.debug("Refreshing app cache.");
        appCache.init();
    }
}