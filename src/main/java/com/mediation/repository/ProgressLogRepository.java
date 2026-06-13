package com.mediation.repository;

import com.mediation.entity.ProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgressLogRepository extends JpaRepository<ProgressLog, Long> {

    List<ProgressLog> findByDisputeIdOrderByLogDateDescCreatedAtDesc(Long disputeId);

    List<ProgressLog> findByMediatorIdOrderByLogDateDesc(Long mediatorId);

    long countByDisputeId(Long disputeId);
}
