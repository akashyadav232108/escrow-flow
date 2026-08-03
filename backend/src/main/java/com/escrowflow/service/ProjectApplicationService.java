package com.escrowflow.service;

import com.escrowflow.domain.Project;
import com.escrowflow.domain.ProjectApplication;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.ApplicationStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.ProjectApplicationRepository;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.ApplicationResponse;
import com.escrowflow.web.dto.ApplyToProjectRequest;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Phase A hiring: freelancers apply to OPEN projects; client accepts one or declines.
 * Instant {@code ProjectService.accept} remains until a later step retires it.
 */
@Service
@Slf4j
public class ProjectApplicationService {

    private static final EnumSet<UserRole> FREELANCER_ROLES =
            EnumSet.of(UserRole.FREELANCER, UserRole.BOTH);

    private final ProjectApplicationRepository applicationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectApplicationService(
            ProjectApplicationRepository applicationRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApplicationResponse apply(Long projectId, ApplyToProjectRequest request) {
        rejectAdminAccess();
        Long freelancerId = SecurityUtils.getCurrentUserId();
        requireFreelancerRole();

        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertCanApply(project, freelancerId);

        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String message = request == null ? null : request.message();
        Instant now = Instant.now();
        ProjectApplication application = applicationRepository.save(ProjectApplication.builder()
                .project(project)
                .freelancer(freelancer)
                .status(ApplicationStatus.PENDING)
                .message(blankToNull(message))
                .createdAt(now)
                .updatedAt(now)
                .build());

        log.info(
                "Application created: id={} projectId={} freelancerId={}",
                application.getId(),
                projectId,
                freelancerId);
        return toResponse(application);
    }

    @Transactional
    public ApplicationResponse accept(Long applicationId) {
        rejectAdminAccess();
        Long clientId = SecurityUtils.getCurrentUserId();

        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        Project project = application.getProject();
        assertClientOwnsProject(project, clientId);
        assertCanAccept(application, project);

        Instant now = Instant.now();
        application.setStatus(ApplicationStatus.ACCEPTED);
        application.setUpdatedAt(now);
        applicationRepository.save(application);

        project.setFreelancer(application.getFreelancer());
        project.setStatus(ProjectStatus.IN_PROGRESS);
        projectRepository.save(project);

        int declined = applicationRepository.declineOtherPending(
                project.getId(), application.getId(), now);

        log.info(
                "Application accepted: id={} projectId={} freelancerId={} othersDeclined={}",
                applicationId,
                project.getId(),
                application.getFreelancer().getId(),
                declined);
        return toResponse(application);
    }

    @Transactional
    public ApplicationResponse decline(Long applicationId) {
        rejectAdminAccess();
        Long clientId = SecurityUtils.getCurrentUserId();

        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        assertClientOwnsProject(application.getProject(), clientId);
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING applications can be declined (status=" + application.getStatus() + ")");
        }

        application.setStatus(ApplicationStatus.DECLINED);
        application.setUpdatedAt(Instant.now());
        ProjectApplication saved = applicationRepository.save(application);

        log.info("Application declined: id={} projectId={}", applicationId, saved.getProject().getId());
        return toResponse(saved);
    }

    @Transactional
    public ApplicationResponse withdraw(Long applicationId) {
        rejectAdminAccess();
        Long freelancerId = SecurityUtils.getCurrentUserId();

        ProjectApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getFreelancer().getId().equals(freelancerId)) {
            throw new ForbiddenException("Only the applicant can withdraw this application");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING applications can be withdrawn (status=" + application.getStatus() + ")");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setUpdatedAt(Instant.now());
        ProjectApplication saved = applicationRepository.save(application);

        log.info("Application withdrawn: id={} projectId={}", applicationId, saved.getProject().getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listForProject(Long projectId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isClient = project.getClient().getId().equals(userId);
        boolean isAssignedFreelancer = project.getFreelancer() != null
                && project.getFreelancer().getId().equals(userId);
        if (!isClient && !isAssignedFreelancer) {
            throw new ForbiddenException("Only the project client can list applications");
        }

        return applicationRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listMine() {
        return applicationRepository
                .findByFreelancerIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    void assertCanApply(Project project, Long freelancerId) {
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new IllegalStateException("Project is not open for applications");
        }
        if (project.getFreelancer() != null) {
            throw new IllegalStateException("Project already has an assigned freelancer");
        }
        if (project.getClient().getId().equals(freelancerId)) {
            throw new ForbiddenException("Cannot apply to your own project");
        }
        if (applicationRepository.existsByProjectIdAndFreelancerId(project.getId(), freelancerId)) {
            throw new IllegalStateException("You have already applied to this project");
        }
    }

    void assertCanAccept(ProjectApplication application, Project project) {
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING applications can be accepted (status=" + application.getStatus() + ")");
        }
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new IllegalStateException("Project is not open for acceptance");
        }
        if (project.getFreelancer() != null) {
            throw new IllegalStateException("Project already has an assigned freelancer");
        }
    }

    private void assertClientOwnsProject(Project project, Long clientId) {
        if (!project.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("Only the project client can manage applications");
        }
    }

    private void requireFreelancerRole() {
        UserRole role = SecurityUtils.getCurrentRole();
        if (!FREELANCER_ROLES.contains(role)) {
            throw new ForbiddenException("Only freelancers can apply to projects");
        }
    }

    private void rejectAdminAccess() {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            throw new ForbiddenException("Admins cannot manage project applications");
        }
    }

    private ApplicationResponse toResponse(ProjectApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getProject().getId(),
                application.getProject().getTitle(),
                application.getFreelancer().getId(),
                application.getFreelancer().getName(),
                application.getStatus(),
                application.getMessage(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private static String blankToNull(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return message.trim();
    }
}
