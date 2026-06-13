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

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Page<Dispute> findByStatus(DisputeStatus status, Pageable pageable);

    Page<Dispute> findByMediatorId(Long mediatorId, Pageable pageable);

    Page<Dispute> findByDisputeType(DisputeType disputeType, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.applicantName LIKE %:keyword%")
    Page<Dispute> searchByApplicantName(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(DisputeStatus status);

    long countByDisputeType(DisputeType disputeType);

    @Query("SELECT d FROM Dispute d WHERE d.status IN :statuses AND d.acceptDate IS NOT NULL")
    List<Dispute> findActiveDisputes(@Param("statuses") List<DisputeStatus> statuses);

    @Query("SELECT d FROM Dispute d WHERE d.status IN :statuses AND d.listingSupervised = true")
    List<Dispute> findListingSupervisedDisputes(@Param("statuses") List<DisputeStatus> statuses);

    @Query("SELECT d.mediatorId, COUNT(d) FROM Dispute d WHERE d.mediatorId IS NOT NULL GROUP BY d.mediatorId")
    List<Object[]> countByMediator();

    @Query("SELECT d.disputeType, COUNT(d), d.acceptDate, d.closeDate FROM Dispute d " +
           "WHERE d.status IN ('调解成功', '调解失败', '已终止') AND d.acceptDate IS NOT NULL AND d.closeDate IS NOT NULL " +
           "GROUP BY d.disputeType, d.acceptDate, d.closeDate")
    List<Object[]> findClosedDisputesForStats();

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status IN ('调解成功', '调解失败', '已终止') " +
           "AND d.acceptDate IS NOT NULL AND d.closeDate IS NOT NULL " +
           "AND d.disputeType = :type")
    long countClosedByType(@Param("type") DisputeType type);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status IN ('调解成功', '调解失败', '已终止') " +
           "AND d.acceptDate IS NOT NULL AND d.closeDate IS NOT NULL " +
           "AND d.timeLimitDays IS NOT NULL " +
           "AND DATEDIFF(d.closeDate, d.acceptDate) > (d.timeLimitDays + COALESCE(d.extensionDays, 0))")
    long countOverdueClosed();

    @Query("SELECT d FROM Dispute d WHERE d.status IN ('调解成功', '调解失败', '已终止') " +
           "AND d.acceptDate IS NOT NULL AND d.closeDate IS NOT NULL")
    List<Dispute> findAllClosedWithDates();
}
