package com.escrowflow.repository;

import com.escrowflow.domain.ProjectApplication;
import com.escrowflow.domain.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {

    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);

    Optional<ProjectApplication> findByProjectIdAndFreelancerId(Long projectId, Long freelancerId);

    @EntityGraph(attributePaths = {"freelancer", "project", "project.client"})
    Optional<ProjectApplication> findById(Long id);

    @EntityGraph(attributePaths = {"freelancer", "project"})
    List<ProjectApplication> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    @EntityGraph(attributePaths = {"project", "project.client"})
    List<ProjectApplication> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ProjectApplication a
            SET a.status = com.escrowflow.domain.enums.ApplicationStatus.DECLINED,
                a.updatedAt = :now
            WHERE a.project.id = :projectId
              AND a.status = com.escrowflow.domain.enums.ApplicationStatus.PENDING
              AND a.id <> :acceptedId
            """)
    int declineOtherPending(
            @Param("projectId") Long projectId,
            @Param("acceptedId") Long acceptedId,
            @Param("now") Instant now);

    long countByProjectIdAndStatus(Long projectId, ApplicationStatus status);
}
