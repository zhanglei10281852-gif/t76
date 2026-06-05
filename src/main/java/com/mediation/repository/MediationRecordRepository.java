package com.mediation.repository;

import com.mediation.entity.MediationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediationRecordRepository extends JpaRepository<MediationRecord, Long> {

    List<MediationRecord> findByDisputeId(Long disputeId);

    List<MediationRecord> findByDisputeIdOrderByMediationDateDesc(Long disputeId);
}
