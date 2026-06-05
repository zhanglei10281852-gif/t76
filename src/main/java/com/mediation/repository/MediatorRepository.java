package com.mediation.repository;

import com.mediation.entity.Mediator;
import com.mediation.entity.Mediator.MediatorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MediatorRepository extends JpaRepository<Mediator, Long> {

    Page<Mediator> findByStatus(MediatorStatus status, Pageable pageable);

    long countByStatus(MediatorStatus status);

    @Query("SELECT m FROM Mediator m WHERE m.name LIKE %:keyword% OR m.organization LIKE %:keyword%")
    Page<Mediator> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT m FROM Mediator m WHERE m.status = :status AND (m.name LIKE %:keyword% OR m.organization LIKE %:keyword%)")
    Page<Mediator> searchByStatusAndKeyword(@Param("status") MediatorStatus status, @Param("keyword") String keyword, Pageable pageable);

    boolean existsByIdCard(String idCard);
}
