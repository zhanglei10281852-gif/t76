package com.mediation.repository;

import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Dispute.DisputeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Page<Dispute> findByStatus(DisputeStatus status, Pageable pageable);

    Page<Dispute> findByMediatorId(Long mediatorId, Pageable pageable);

    Page<Dispute> findByDisputeType(DisputeType disputeType, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.applicantName LIKE %:keyword%")
    Page<Dispute> searchByApplicantName(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(DisputeStatus status);

    long countByDisputeType(DisputeType disputeType);
}
