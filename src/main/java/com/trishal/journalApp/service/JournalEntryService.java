package com.trishal.journalApp.service;

import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.repository.JournalEntryRepo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class JournalEntryService {

    private static final Logger logger = LoggerFactory.getLogger(JournalEntryService.class);

    @Autowired
    private JournalEntryRepo journalEntryRepo;

    @Autowired
    private UserService userService;

    @Transactional
    public void saveEntry(JournalEntry journalEntry, String userName){
        try {
            User user = userService.findByUserName(userName);
            JournalEntry savedJournalEntry = journalEntryRepo.save(journalEntry);
            user.getJournalEntries().add(savedJournalEntry);
            userService.saveEntry(user);
        }
        catch (Exception e){
            logger.error("Exception: ", e);
            throw new RuntimeException("An error occured!");
        }
    }

    public void saveEntry(JournalEntry journalEntry){
        try {
            journalEntryRepo.save(journalEntry);
        }
        catch (Exception e){
            logger.error("Exception: ", e);
            throw new RuntimeException("An error occured!");
        }
    }

    public List<JournalEntry> getAll(){
        return journalEntryRepo.findAll();
    }

    public Optional<JournalEntry> getJournalEntryById(UUID id){
        return journalEntryRepo.findById(id);
    }

    @Transactional
    public boolean deleteJournalEntryById(UUID id, String userName){
        boolean removed = false;
        try{
            User user = userService.findByUserName(userName);
            removed = user.getJournalEntries().removeIf(journalEntry -> journalEntry.getId().equals(id));
            if (removed){
                userService.saveEntry(user);
                journalEntryRepo.deleteById(id);
            }
        }
        catch (Exception e){
            logger.error("Exception: ", e);
            throw new RuntimeException("An error occured while deleting journal entry!");
        }
        return removed;
    }

}
