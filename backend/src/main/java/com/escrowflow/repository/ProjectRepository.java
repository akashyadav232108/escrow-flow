package com.escrowflow.repository;

import com.escrowflow.domain.Project;
import com.escrowflow.domain.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
            SELECT p FROM Project p
            WHERE (:status IS NULL OR p.status = :status)
              AND p.client.id = :userId
            ORDER BY p.createdAt DESC
            """)
    List<Project> findByClient(@Param("userId") Long userId, @Param("status") ProjectStatus status);

    @Query("""
            SELECT p FROM Project p
            WHERE (:status IS NULL OR p.status = :status)
              AND (p.freelancer.id = :userId OR p.status = com.escrowflow.domain.enums.ProjectStatus.OPEN)
            ORDER BY p.createdAt DESC
            """)
    List<Project> findForFreelancerView(@Param("userId") Long userId, @Param("status") ProjectStatus status);

    @Query("""
            SELECT DISTINCT p FROM Project p
            WHERE (:status IS NULL OR p.status = :status)
              AND (p.client.id = :userId OR p.freelancer.id = :userId
                   OR p.status = com.escrowflow.domain.enums.ProjectStatus.OPEN)
            ORDER BY p.createdAt DESC
            """)
    List<Project> findForBothRoles(@Param("userId") Long userId, @Param("status") ProjectStatus status);

    @Query("""
            SELECT p FROM Project p
            LEFT JOIN FETCH p.client
            LEFT JOIN FETCH p.freelancer
            LEFT JOIN FETCH p.milestones
            WHERE p.id = :id
            """)
    Optional<Project> findByIdWithDetails(@Param("id") Long id);
}
