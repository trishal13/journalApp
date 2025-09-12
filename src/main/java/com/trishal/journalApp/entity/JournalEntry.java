package com.trishal.journalApp.entity;

import com.trishal.journalApp.enums.Sentiment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
//@Getter
//@Setter
@AllArgsConstructor
@NoArgsConstructor
//@EqualsAndHashCode
//@ToString
//@Builder
@Data
public class JournalEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @NonNull
    private String title;

    private String content;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Sentiment sentiment;
}
