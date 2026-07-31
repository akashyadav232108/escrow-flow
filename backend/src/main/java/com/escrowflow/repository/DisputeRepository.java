package com.escrowflow.repository;

import com.escrowflow.domain.Dispute;
import com.escrowflow.domain.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByMilestoneId(Long milestoneId);

    boolean existsByMilestoneId(Long milestoneId);

    long countByStatus(DisputeStatus status);

    @Query("""
            SELECT d FROM Dispute d
            JOIN FETCH d.milestone m
            JOIN FETCH m.project p
            JOIN FETCH p.client
            LEFT JOIN FETCH p.freelancer
            JOIN FETCH d.raisedBy
            LEFT JOIN FETCH d.resolvedBy
            WHERE (:status IS NULL OR d.status = :status)
            ORDER BY d.createdAt DESC
            """)
    List<Dispute> findAllWithDetails(@Param("status") DisputeStatus status);

    @Query("""
            SELECT d FROM Dispute d
            JOIN FETCH d.milestone m
            JOIN FETCH m.project p
            JOIN FETCH p.client
            LEFT JOIN FETCH p.freelancer
            JOIN FETCH d.raisedBy
            LEFT JOIN FETCH d.resolvedBy
            WHERE d.id = :id
            """)
    Optional<Dispute> findByIdWithDetails(@Param("id") Long id);
}
