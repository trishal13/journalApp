package com.trishal.journalApp.scheduler;

import com.trishal.journalApp.cache.AppCache;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.enums.Sentiment;
import com.trishal.journalApp.model.SentimentData;
import com.trishal.journalApp.repository.impl.UserRepoImpl;
import com.trishal.journalApp.service.EmailService;
import com.trishal.journalApp.service.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class UserScheduler {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepoImpl userRepoImpl;

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    private AppCache appCache;

    @Autowired
    private KafkaTemplate<String, SentimentData> kafkaTemplate;

    @Scheduled(cron = "0 0 9 * * SUN")
    public void fetchUsersAndSendSAMail(){
        List<User> users = userRepoImpl.getUsersForSentimentAnalysis();
        for (User user: users){
            List<JournalEntry> journalEntries = user.getJournalEntries();
            List<Sentiment> sentiments = journalEntries.stream()
                    .filter(x -> x.getDate().toInstant()
                            .isAfter(LocalDateTime.now()
                                    .minus(7, ChronoUnit.DAYS)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()))
                    .map(x -> x.getSentiment())
                    .collect(Collectors.toList());
            Map<Sentiment, Integer> sentimentCounts = new HashMap<>();
            for (Sentiment sentiment: sentiments){
                if (!Objects.isNull(sentiment)){
                    sentimentCounts.put(sentiment, sentimentCounts.getOrDefault(sentiment,0)+1);
                }
            }
            Sentiment mostFrequentSentiment = null;
            int maxCount = 0;
            for (Map.Entry<Sentiment, Integer> entry: sentimentCounts.entrySet()){
                if (entry.getValue() > maxCount){
                    maxCount=entry.getValue();
                    mostFrequentSentiment = entry.getKey();
                }
            }
            if (!Objects.isNull(mostFrequentSentiment)){
                SentimentData sentimentData = SentimentData.builder()
                                .email(user.getEmail())
                                        .sentiment("Sentiment for last 7 days" + mostFrequentSentiment.toString())
                                                .build();
                try{
                    kafkaTemplate.send("weekly-sentiments", sentimentData.getEmail(), sentimentData);
                }
                catch (Exception e){
                    emailService.sendEmail(sentimentData.getEmail(), "SentimentData for previous week", sentimentData.getSentiment());
                }
            }
        }
    }

    @Scheduled(cron = "0 0/10 * ? * *")
    public void clearAppCache(){
        appCache.init();
    }

}
