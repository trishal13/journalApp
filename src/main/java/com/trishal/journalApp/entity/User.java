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
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<JournalEntry> journalEntries = new ArrayList<>();

    /**
     * BUG FIX: Removed the stale `roles` column from the `users` table.
     *
     * Root cause: at some point Hibernate auto-created a `roles` column (Postgres
     * array type) in the `users` table in addition to the proper `user_roles` join
     * table that @ElementCollection produces. JPA ONLY reads/writes the join table —
     * the `users.roles` column was never populated by the application, causing a
     * confusing situation where `users.roles` showed data but the runtime user had
     * no roles.
     *
     * Fix: this @ElementCollection mapping is the single source of truth. The stale
     * column must be removed from the database with the migration script below.
     *
     * @Builder.Default ensures the builder initialises this as an empty list
     * rather than null when roles are not explicitly set.
     */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();
}