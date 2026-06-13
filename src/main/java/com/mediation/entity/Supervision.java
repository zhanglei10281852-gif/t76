package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "supervisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supervision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "mediator_id")
    private Long mediatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private SupervisionLevel level;

    @Column(name = "supervisor_id")
    private Long supervisorId;

    @Column(name = "supervisor_name")
    private String supervisorName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "required_close_date", nullable = false)
    private LocalDate requiredCloseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupervisionStatus status;

    @Column(name = "reply_expected_date")
    private LocalDate replyExpectedDate;

    @Column(columnDefinition = "TEXT")
    private String replyProgress;

    @Column(columnDefinition = "TEXT")
    private String replyDelayReason;

    @Column(name = "reply_time")
    private LocalDateTime replyTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SupervisionStatus.待回复;
        }
    }

    public enum SupervisionLevel {
        一般, 紧急
    }

    public enum SupervisionStatus {
        待回复, 已回复, 已解除
    }
}
