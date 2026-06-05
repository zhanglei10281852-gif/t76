package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mediators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mediator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "id_card", unique = true, nullable = false, length = 18)
    private String idCard;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String organization;

    private String speciality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediatorLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediatorStatus status;

    @Column(name = "case_count")
    private int caseCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MediatorStatus.在岗;
        }
    }

    public enum MediatorLevel {
        初级, 中级, 高级
    }

    public enum MediatorStatus {
        在岗, 休假, 离职
    }
}
