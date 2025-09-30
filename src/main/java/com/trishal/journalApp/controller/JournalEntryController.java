package com.trishal.journalApp.controller;

import com.trishal.journalApp.dto.JournalEntryCreateRequestDto;
import com.trishal.journalApp.dto.JournalEntryResponseDto;
import com.trishal.journalApp.dto.JournalEntryUpdateRequestDto;
import com.trishal.journalApp.dto.MessageResponseDto;
import com.trishal.journalApp.entity.JournalEntry;
import com.trishal.journalApp.entity.User;
import com.trishal.journalApp.mapper.JournalEntryMapper;
import com.trishal.journalApp.service.JournalEntryService;
import com.trishal.journalApp.service.UserService;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/journal")
public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private UserService userService;

    @Autowired
    private JournalEntryMapper journalEntryMapper;

    @GetMapping
    public ResponseEntity<List<JournalEntryResponseDto>> getAllJournalEntriesOfUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUserName(userName);
        List<JournalEntry> journalEntries = user.getJournalEntries();
        if (Objects.isNull(journalEntries) || journalEntries.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<JournalEntryResponseDto> journalEntryResponseDtoList = journalEntryMapper.toResponseList(journalEntries);
        return new ResponseEntity<>(journalEntryResponseDtoList, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<JournalEntryResponseDto> createEntry(@RequestBody JournalEntryCreateRequestDto journalEntryCreateRequestDto){
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();
            JournalEntry newJournalEntry = journalEntryMapper.toEntity(journalEntryCreateRequestDto);
            journalEntryService.saveEntry(newJournalEntry, userName);
            JournalEntryResponseDto journalEntryResponseDto = journalEntryMapper.toResponse(newJournalEntry);
            return new ResponseEntity<>(journalEntryResponseDto, HttpStatus.CREATED);
        }
        catch (Exception e){
            log.error("Error occured ", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<JournalEntryResponseDto> getJournalEntryById(@PathVariable UUID id){
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
        JournalEntryResponseDto journalEntryResponseDto = journalEntryMapper.toResponse(journalEntry);
        return new ResponseEntity<>(journalEntryResponseDto, HttpStatus.OK);
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<MessageResponseDto> deleteJournalEntryById(@PathVariable UUID id){
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
            return new ResponseEntity<>(MessageResponseDto.builder().message("Journal Entry deleted successfully!").success(true).build(), HttpStatus.OK);
        }
        return new ResponseEntity<>(MessageResponseDto.builder().message("No journal entry found!").success(false).build(), HttpStatus.NOT_FOUND);
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<JournalEntryResponseDto> updateJournalEntryById(
            @PathVariable UUID id,
            @RequestBody JournalEntryUpdateRequestDto journalEntryUpdateRequestDto
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
        if (!Objects.isNull(journalEntryUpdateRequestDto.getTitle()) && !journalEntryUpdateRequestDto.getTitle().isEmpty()){
            oldJournalEntry.setTitle(journalEntryUpdateRequestDto.getTitle());
        }
        if (!Objects.isNull(journalEntryUpdateRequestDto.getContent()) && !journalEntryUpdateRequestDto.getContent().isEmpty()){
            oldJournalEntry.setContent(journalEntryUpdateRequestDto.getContent());
        }
        journalEntryService.saveEntry(oldJournalEntry);
        JournalEntryResponseDto journalEntryResponseDto = journalEntryMapper.toResponse(oldJournalEntry);
        return new ResponseEntity<>(journalEntryResponseDto, HttpStatus.OK);
    }

}
