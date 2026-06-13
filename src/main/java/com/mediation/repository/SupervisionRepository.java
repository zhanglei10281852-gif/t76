package com.mediation.repository;

import com.mediation.entity.Supervision;
import com.mediation.entity.Supervision.SupervisionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SupervisionRepository extends JpaRepository<Supervision, Long> {

    List<Supervision> findByDisputeIdOrderByCreatedAtDesc(Long disputeId);

    List<Supervision> findByMediatorIdOrderByCreatedAtDesc(Long mediatorId);

    Page<Supervision> findByStatus(SupervisionStatus status, Pageable pageable);

    long countByDisputeId(Long disputeId);

    long countByDisputeIdAndStatus(Long disputeId, SupervisionStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Supervision s SET s.status = :status WHERE s.disputeId = :disputeId AND s.status != :excludeStatus")
    int updateStatusByDisputeId(@Param("disputeId") Long disputeId,
                                @Param("status") SupervisionStatus status,
                                @Param("excludeStatus") SupervisionStatus excludeStatus);

    @Query("SELECT s.disputeId, COUNT(s) FROM Supervision s GROUP BY s.disputeId")
    List<Object[]> countByDispute();
}
