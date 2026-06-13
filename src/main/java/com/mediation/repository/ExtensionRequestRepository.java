package com.mediation.repository;

import com.mediation.entity.ExtensionRequest;
import com.mediation.entity.ExtensionRequest.ExtensionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExtensionRequestRepository extends JpaRepository<ExtensionRequest, Long> {

    List<ExtensionRequest> findByDisputeIdOrderByCreatedAtDesc(Long disputeId);

    List<ExtensionRequest> findByMediatorIdOrderByCreatedAtDesc(Long mediatorId);

    Page<ExtensionRequest> findByStatus(ExtensionStatus status, Pageable pageable);

    Optional<ExtensionRequest> findFirstByDisputeIdOrderByCreatedAtDesc(Long disputeId);

    long countByStatus(ExtensionStatus status);

    boolean existsByDisputeIdAndStatus(Long disputeId, ExtensionStatus status);
}
