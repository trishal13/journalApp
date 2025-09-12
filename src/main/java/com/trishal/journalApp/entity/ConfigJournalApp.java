package com.trishal.journalApp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "config_journal_app")
@Data
@NoArgsConstructor
public class ConfigJournalApp {

    @Id
    @GeneratedValue
    private UUID id;

    private String key;
    private String value;

}
