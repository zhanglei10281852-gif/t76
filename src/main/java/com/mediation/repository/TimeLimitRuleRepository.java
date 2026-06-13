package com.mediation.repository;

import com.mediation.entity.Dispute;
import com.mediation.entity.TimeLimitRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimeLimitRuleRepository extends JpaRepository<TimeLimitRule, Long> {

    Optional<TimeLimitRule> findByDisputeType(Dispute.DisputeType disputeType);

    void deleteByDisputeType(Dispute.DisputeType disputeType);

    boolean existsByDisputeType(Dispute.DisputeType disputeType);
}
