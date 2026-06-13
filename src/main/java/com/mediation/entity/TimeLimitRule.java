package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_limit_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLimitRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", unique = true, nullable = false)
    private Dispute.DisputeType disputeType;

    @Column(name = "limit_days", nullable = false)
    private Integer limitDays;

    @Column(name = "amount_threshold", precision = 12, scale = 2)
    @Builder.Default
    private java.math.BigDecimal amountThreshold = new java.math.BigDecimal("100000");

    @Column(name = "amount_extra_days")
    @Builder.Default
    private Integer amountExtraDays = 15;

    @Column(name = "max_extension_days")
    @Builder.Default
    private Integer maxExtensionDays = 15;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
