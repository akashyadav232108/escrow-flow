package com.escrowflow.service;

import com.escrowflow.domain.Project;
import com.escrowflow.domain.ProjectAgreement;
import com.escrowflow.repository.ProjectAgreementRepository;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.ProjectAgreementResponse;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Hire-time project agreement. Both parties acknowledge shared terms (no automatic penalties).
 * Milestone money actions stay blocked until both have accepted.
 */
@Service
@Slf4j
public class ProjectAgreementService {

    public static final String TERMS_VERSION = "1.0";

    public static final String DEFAULT_TERMS = """
            Escrow-Flow Project Agreement (v1.0)

            1. Scope — Work is defined by this project's milestones and descriptions.
            2. Escrow — Client funds milestones before work; funds release on approval or per dispute/exit rules.
            3. Delivery — Freelancer submits completed work for each funded milestone in good faith.
            4. Disputes — Milestone disputes and project exits are reviewed by platform admins.
            5. Settlements — Admin decisions on held escrow (including splits) are final for platform purposes.
            6. Evidence — These accepted terms may be referenced by either party during disputes or exits.
            7. No automatic penalties — Accepting these terms does not authorize automatic fines; money moves only via escrow, approval, dispute, or exit settlement.

            By accepting, you confirm you have read and agree to these terms for this project.
            """.stripIndent().trim();

    private final ProjectAgreementRepository agreementRepository;
    private final ProjectRepository projectRepository;

    public ProjectAgreementService(
            ProjectAgreementRepository agreementRepository,
            ProjectRepository projectRepository) {
        this.agreementRepository = agreementRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectAgreement createOnHire(Project project, Instant clientAcceptedAt) {
        if (agreementRepository.existsByProjectId(project.getId())) {
            throw new IllegalStateException("An agreement already exists for this project");
        }
        ProjectAgreement agreement = agreementRepository.save(ProjectAgreement.builder()
                .project(project)
                .termsVersion(TERMS_VERSION)
                .termsText(DEFAULT_TERMS)
                .clientAcceptedAt(clientAcceptedAt)
                .freelancerAcceptedAt(null)
                .createdAt(Instant.now())
                .build());
        log.info("Project agreement created: id={} projectId={}", agreement.getId(), project.getId());
        return agreement;
    }

    @Transactional(readOnly = true)
    public ProjectAgreementResponse getForProject(Long projectId) {
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        assertCanView(project);
        ProjectAgreement agreement = agreementRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project agreement not found"));
        return toResponse(agreement);
    }

    @Transactional
    public ProjectAgreementResponse accept(Long projectId) {
        rejectAdminAccess();
        Long userId = SecurityUtils.getCurrentUserId();
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        ProjectAgreement agreement = agreementRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project agreement not found"));

        Instant now = Instant.now();
        boolean isClient = project.getClient().getId().equals(userId);
        boolean isFreelancer = project.getFreelancer() != null
                && project.getFreelancer().getId().equals(userId);

        if (!isClient && !isFreelancer) {
            throw new ForbiddenException("Only the project client or freelancer can accept the agreement");
        }

        if (isClient) {
            if (agreement.getClientAcceptedAt() != null) {
                throw new IllegalStateException("Client has already accepted the agreement");
            }
            agreement.setClientAcceptedAt(now);
        } else {
            if (agreement.getFreelancerAcceptedAt() != null) {
                throw new IllegalStateException("Freelancer has already accepted the agreement");
            }
            agreement.setFreelancerAcceptedAt(now);
        }

        ProjectAgreement saved = agreementRepository.save(agreement);
        log.info(
                "Project agreement accepted: projectId={} userId={} fullyAccepted={}",
                projectId,
                userId,
                saved.isFullyAccepted());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public void requireFullyAccepted(Long projectId) {
        ProjectAgreement agreement = agreementRepository.findByProjectId(projectId).orElse(null);
        // Legacy projects hired before agreements existed may continue without a row.
        if (agreement == null) {
            return;
        }
        if (!agreement.isFullyAccepted()) {
            throw new IllegalStateException(
                    "Both parties must accept the project agreement before continuing with milestone work");
        }
    }

    @Transactional
    public void deleteForProject(Long projectId) {
        agreementRepository.deleteByProjectId(projectId);
    }

    private void assertCanView(Project project) {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isClient = project.getClient().getId().equals(userId);
        boolean isFreelancer = project.getFreelancer() != null
                && project.getFreelancer().getId().equals(userId);
        if (!isClient && !isFreelancer) {
            throw new ForbiddenException("Not authorized to view this project agreement");
        }
    }

    private void rejectAdminAccess() {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            throw new ForbiddenException("Admins cannot accept project agreements");
        }
    }

    private ProjectAgreementResponse toResponse(ProjectAgreement agreement) {
        return new ProjectAgreementResponse(
                agreement.getId(),
                agreement.getProject().getId(),
                agreement.getTermsVersion(),
                agreement.getTermsText(),
                agreement.getClientAcceptedAt(),
                agreement.getFreelancerAcceptedAt(),
                agreement.getClientAcceptedAt() != null,
                agreement.getFreelancerAcceptedAt() != null,
                agreement.isFullyAccepted(),
                agreement.getCreatedAt());
    }
}
