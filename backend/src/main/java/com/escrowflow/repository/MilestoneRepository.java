package com.escrowflow.repository;

import com.escrowflow.domain.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    @Query("""
            SELECT m FROM Milestone m
            LEFT JOIN FETCH m.project p
            LEFT JOIN FETCH p.client
            LEFT JOIN FETCH p.freelancer
            WHERE m.id = :id
            """)
    Optional<Milestone> findByIdWithProject(@Param("id") Long id);
}
