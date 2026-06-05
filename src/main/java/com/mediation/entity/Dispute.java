package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_no", unique = true, nullable = false)
    private String caseNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false)
    private DisputeType disputeType;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(name = "applicant_phone", nullable = false)
    private String applicantPhone;

    @Column(name = "applicant_id_card", nullable = false)
    private String applicantIdCard;

    @Column(name = "respondent_name", nullable = false)
    private String respondentName;

    @Column(name = "respondent_phone")
    private String respondentPhone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "mediator_id")
    private Long mediatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DisputeStatus.待受理;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum DisputeType {
        邻里纠纷, 婚姻家庭, 劳动争议, 合同纠纷, 损害赔偿, 土地权属, 其他
    }

    public enum DisputeStatus {
        待受理, 已受理, 调解中, 调解成功, 调解失败, 已撤回
    }
}
