package com.trishal.journalApp.controller;

import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.service.JournalEntryService;
import com.trishal.journalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/journal")
public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<JournalEntry>> getAllJournalEntriesOfUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUserName(userName);
        List<JournalEntry> journalEntries = user.getJournalEntries();
        if (Objects.isNull(journalEntries) || journalEntries.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(journalEntries, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JournalEntry> createEntry(@RequestBody JournalEntry newJournalEntry){
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();
            journalEntryService.saveEntry(newJournalEntry, userName);
            return new ResponseEntity<>(newJournalEntry, HttpStatus.CREATED);
        }
        catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<JournalEntry> getJournalEntryById(@PathVariable UUID id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUserName(userName);
        List<JournalEntry> collect = user.getJournalEntries()
                .stream()
                .filter(journalEntry -> journalEntry.getId().equals(id)).collect(Collectors.toList());
        if (collect.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        JournalEntry journalEntry = journalEntryService.getJournalEntryById(id).orElse(null);
        if (Objects.isNull(journalEntry)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(journalEntry, HttpStatus.OK);
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<JournalEntry> deleteJournalEntryById(@PathVariable UUID id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUserName(userName);
        List<JournalEntry> collect = user.getJournalEntries()
                .stream()
                .filter(journalEntry -> journalEntry.getId().equals(id)).collect(Collectors.toList());
        if (collect.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        boolean removed = journalEntryService.deleteJournalEntryById(id, userName);
        if (removed){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<JournalEntry> updateJournalEntryById(
            @PathVariable UUID id,
            @RequestBody JournalEntry newJournalEntry
    ){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUserName(userName);
        List<JournalEntry> collect = user.getJournalEntries()
                .stream()
                .filter(journalEntry -> journalEntry.getId().equals(id)).collect(Collectors.toList());
        if (collect.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        JournalEntry oldJournalEntry = journalEntryService.getJournalEntryById(id).orElse(null);
        if (Objects.isNull(oldJournalEntry)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!Objects.isNull(newJournalEntry.getTitle()) && !newJournalEntry.getTitle().isEmpty()){
            oldJournalEntry.setTitle(newJournalEntry.getTitle());
        }
        if (!Objects.isNull(newJournalEntry.getContent()) && !newJournalEntry.getContent().isEmpty()){
            oldJournalEntry.setContent(newJournalEntry.getContent());
        }
        journalEntryService.saveEntry(oldJournalEntry);
        return new ResponseEntity<>(oldJournalEntry, HttpStatus.OK);
    }

}
