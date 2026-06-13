package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "extension_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    @Column(name = "mediator_id", nullable = false)
    private Long mediatorId;

    @Column(name = "request_days", nullable = false)
    private Integer requestDays;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtensionStatus status;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approver_name")
    private String approverName;

    @Column(columnDefinition = "TEXT")
    private String approveRemark;

    @Column(name = "approved_days")
    private Integer approvedDays;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ExtensionStatus.待审批;
        }
    }

    public enum ExtensionStatus {
        待审批, 已批准, 已拒绝
    }
}
