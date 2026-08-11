package com.escrowflow.repository;

import com.escrowflow.domain.ProjectExit;
import com.escrowflow.domain.enums.ProjectExitStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectExitRepository extends JpaRepository<ProjectExit, Long> {

    boolean existsByProjectIdAndStatus(Long projectId, ProjectExitStatus status);

    Optional<ProjectExit> findByProjectIdAndStatus(Long projectId, ProjectExitStatus status);

    @EntityGraph(attributePaths = {
            "project", "project.client", "project.freelancer",
            "raisedBy", "resolvedBy", "settlements", "settlements.milestone"
    })
    @Query("SELECT e FROM ProjectExit e WHERE e.id = :id")
    Optional<ProjectExit> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "project", "project.client", "project.freelancer",
            "raisedBy", "resolvedBy", "settlements", "settlements.milestone"
    })
    @Query("""
            SELECT e FROM ProjectExit e
            WHERE (:status IS NULL OR e.status = :status)
            ORDER BY e.createdAt DESC
            """)
    List<ProjectExit> findAllWithDetails(@Param("status") ProjectExitStatus status);
}
