package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mediation_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "mediation_date", nullable = false)
    private LocalDate mediationDate;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String attendees;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String outcome;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
