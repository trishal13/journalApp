package com.trishal.journalApp.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private UUID userId;

    @Column(unique = true, nullable = false)
    @NonNull
    private String userName;

    @Column(nullable = false)
    @NonNull
    private String password;

    private String email;

    private boolean sentimentAnalysis;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntry> journalEntries = new ArrayList<>();

    private List<String> roles;
}
