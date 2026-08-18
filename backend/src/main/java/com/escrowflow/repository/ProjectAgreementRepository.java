package com.escrowflow.repository;

import com.escrowflow.domain.ProjectAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectAgreementRepository extends JpaRepository<ProjectAgreement, Long> {

    Optional<ProjectAgreement> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
