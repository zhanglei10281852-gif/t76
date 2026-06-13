package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "listing_supervisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingSupervision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", unique = true, nullable = false)
    private Long disputeId;

    @Column(name = "leader_id")
    private Long leaderId;

    @Column(name = "leader_name")
    private String leaderName;

    @Column(name = "upgrade_time")
    private LocalDateTime upgradeTime;

    @Column(columnDefinition = "TEXT")
    private String upgradeReason;

    @Enumerated(EnumType.STRING)
    @Column
    private ListingAction action;

    @Column(name = "new_mediator_id")
    private Long newMediatorId;

    @Column(name = "new_mediator_name")
    private String newMediatorName;

    @Column(name = "action_time")
    private LocalDateTime actionTime;

    @Column(columnDefinition = "TEXT")
    private String actionRemark;

    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    @Column(columnDefinition = "TEXT")
    private String meetingAttendees;

    @Column(columnDefinition = "TEXT")
    private String meetingResolution;

    @Enumerated(EnumType.STRING)
    @Column
    private ListingStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ListingStatus.待处理;
        }
        if (this.upgradeTime == null) {
            this.upgradeTime = LocalDateTime.now();
        }
    }

    public enum ListingAction {
        更换调解员, 召开协调会, 终止调解
    }

    public enum ListingStatus {
        待处理, 处理中, 已完成
    }
}
