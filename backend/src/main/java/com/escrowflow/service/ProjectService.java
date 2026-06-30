package com.escrowflow.service;

import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.security.UserPrincipal;
import com.escrowflow.web.dto.CreateProjectRequest;
import com.escrowflow.web.dto.MilestoneResponse;
import com.escrowflow.web.dto.ProjectDetailResponse;
import com.escrowflow.web.dto.ProjectSummaryResponse;
import com.escrowflow.web.dto.ProjectUserSummary;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectDetailResponse create(CreateProjectRequest request) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        requireClientRole(principal);

        User client = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = Project.builder()
                .client(client)
                .title(request.title())
                .description(request.description())
                .status(ProjectStatus.OPEN)
                .build();

        for (CreateProjectRequest.CreateMilestoneRequest milestoneRequest : request.milestones()) {
            Milestone milestone = Milestone.builder()
                    .project(project)
                    .title(milestoneRequest.title())
                    .description(milestoneRequest.description())
                    .amount(milestoneRequest.amount())
                    .status(MilestoneStatus.PENDING)
                    .build();
            project.getMilestones().add(milestone);
        }

        Project saved = projectRepository.save(project);
        log.info("Project created: projectId={} clientId={} milestoneCount={}",
                saved.getId(), client.getId(), saved.getMilestones().size());

        return toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listForUser(ProjectStatus status) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        List<Project> projects = switch (principal.getRole()) {
            case CLIENT -> projectRepository.findByClient(principal.getUserId(), status);
            case FREELANCER -> projectRepository.findForFreelancerView(principal.getUserId(), status);
            case BOTH -> projectRepository.findForBothRoles(principal.getUserId(), status);
        };
        return projects.stream().map(this::toSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getById(Long projectId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        requireCanView(project, principal);
        return toDetailResponse(project);
    }

    @Transactional
    public ProjectDetailResponse accept(Long projectId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        requireFreelancerRole(principal);

        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new IllegalStateException("Project is not open for acceptance");
        }
        if (project.getFreelancer() != null) {
            throw new IllegalStateException("Project already has a freelancer");
        }
        if (project.getClient().getId().equals(principal.getUserId())) {
            throw new ForbiddenException("Cannot accept your own project");
        }

        User freelancer = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        project.setFreelancer(freelancer);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        Project saved = projectRepository.save(project);

        log.info("Project accepted: projectId={} freelancerId={}", saved.getId(), freelancer.getId());
        return toDetailResponse(saved);
    }

    private void requireClientRole(UserPrincipal principal) {
        if (principal.getRole() != UserRole.CLIENT && principal.getRole() != UserRole.BOTH) {
            throw new ForbiddenException("Only clients can create projects");
        }
    }

    private void requireFreelancerRole(UserPrincipal principal) {
        if (principal.getRole() != UserRole.FREELANCER && principal.getRole() != UserRole.BOTH) {
            throw new ForbiddenException("Only freelancers can accept projects");
        }
    }

    private void requireCanView(Project project, UserPrincipal principal) {
        Long userId = principal.getUserId();
        boolean isClient = project.getClient().getId().equals(userId);
        boolean isFreelancer = project.getFreelancer() != null
                && project.getFreelancer().getId().equals(userId);
        boolean isOpen = project.getStatus() == ProjectStatus.OPEN;

        if (!isClient && !isFreelancer && !isOpen) {
            throw new ForbiddenException("Not authorized to view this project");
        }
    }

    private ProjectSummaryResponse toSummaryResponse(Project project) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getTitle(),
                project.getStatus(),
                toUserSummary(project.getClient()),
                project.getFreelancer() != null ? toUserSummary(project.getFreelancer()) : null,
                project.getCreatedAt());
    }

    private ProjectDetailResponse toDetailResponse(Project project) {
        List<MilestoneResponse> milestones = project.getMilestones().stream()
                .map(this::toMilestoneResponse)
                .toList();
        return new ProjectDetailResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                toUserSummary(project.getClient()),
                project.getFreelancer() != null ? toUserSummary(project.getFreelancer()) : null,
                milestones,
                project.getCreatedAt());
    }

    private MilestoneResponse toMilestoneResponse(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getAmount(),
                milestone.getStatus());
    }

    private ProjectUserSummary toUserSummary(User user) {
        return new ProjectUserSummary(user.getId(), user.getName());
    }
}
