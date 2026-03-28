package com.trishal.journalApp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "config_journal_app")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigJournalApp {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * KEY is a reserved word in SQL. Quoting it prevents issues with
     * databases that reject unquoted reserved words in DDL/DML.
     * The column name in PostgreSQL stays "key" — the quotes are only
     * for the SQL generation layer.
     */
    @Column(name = "\"key\"", nullable = false, unique = true)
    private String key;

    @Column(name = "value")
    private String value;
}