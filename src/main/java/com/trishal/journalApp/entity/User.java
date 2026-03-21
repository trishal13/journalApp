package com.trishal.journalApp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
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

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JournalEntry> journalEntries = new ArrayList<>();

    /**
     * BUG FIX: List<String> must be annotated with @ElementCollection so JPA
     * knows to store it in a separate join table (user_roles).
     * Without this, Hibernate cannot persist or load the roles list.
     *
     * @Builder.Default ensures the builder initialises this as an empty list
     * rather than null when roles are not explicitly set.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();
}