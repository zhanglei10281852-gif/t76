package com.mediation.repository;

import com.mediation.entity.ListingSupervision;
import com.mediation.entity.ListingSupervision.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ListingSupervisionRepository extends JpaRepository<ListingSupervision, Long> {

    Optional<ListingSupervision> findByDisputeId(Long disputeId);

    boolean existsByDisputeId(Long disputeId);

    Page<ListingSupervision> findByStatus(ListingStatus status, Pageable pageable);

    long countByStatus(ListingStatus status);
}
