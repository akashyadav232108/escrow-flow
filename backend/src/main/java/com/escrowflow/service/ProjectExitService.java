package com.escrowflow.service;

import com.escrowflow.domain.EscrowHold;
import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.ProjectExit;
import com.escrowflow.domain.ProjectExitSettlement;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.DisputeStatus;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.NotificationReferenceType;
import com.escrowflow.domain.enums.NotificationType;
import com.escrowflow.domain.enums.ProjectExitOutcome;
import com.escrowflow.domain.enums.ProjectExitStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.DisputeRepository;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.ProjectApplicationRepository;
import com.escrowflow.repository.ProjectExitRepository;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.ProjectExitResponse;
import com.escrowflow.web.dto.RaiseProjectExitRequest;
import com.escrowflow.web.dto.ResolveProjectExitRequest;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phase B: mid-project exit. Admin settles each held milestone (release vs refund amounts)
 * and cancels or reopens the project. Admin decision is final.
 */
@Service
@Slf4j
public class ProjectExitService {

    private static final EnumSet<UserRole> ADMIN_ROLES = EnumSet.of(UserRole.ADMIN, UserRole.SUPER_ADMIN);
    private static final EnumSet<MilestoneStatus> HELD_MILESTONE_STATUSES = EnumSet.of(
            MilestoneStatus.FUNDS_LOCKED,
            MilestoneStatus.SUBMITTED,
            MilestoneStatus.DISPUTED);

    private final ProjectExitRepository projectExitRepository;
    private final ProjectRepository projectRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final DisputeRepository disputeRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectAgreementService projectAgreementService;
    private final UserRepository userRepository;
    private final EscrowService escrowService;
    private final NotificationService notificationService;

    public ProjectExitService(
            ProjectExitRepository projectExitRepository,
            ProjectRepository projectRepository,
            EscrowHoldRepository escrowHoldRepository,
            DisputeRepository disputeRepository,
            ProjectApplicationRepository projectApplicationRepository,
            ProjectAgreementService projectAgreementService,
            UserRepository userRepository,
            EscrowService escrowService,
            NotificationService notificationService) {
        this.projectExitRepository = projectExitRepository;
        this.projectRepository = projectRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.disputeRepository = disputeRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.projectAgreementService = projectAgreementService;
        this.userRepository = userRepository;
        this.escrowService = escrowService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProjectExitResponse raise(Long projectId, RaiseProjectExitRequest request) {
        rejectAdminAccess();
        Long userId = SecurityUtils.getCurrentUserId();

        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertPartyOnProject(project, userId);
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            throw new IllegalStateException("Project exit can only be raised while IN_PROGRESS");
        }
        if (project.getFreelancer() == null) {
            throw new IllegalStateException("Project has no assigned freelancer");
        }
        if (projectExitRepository.existsByProjectIdAndStatus(projectId, ProjectExitStatus.OPEN)) {
            throw new IllegalStateException("An open project exit already exists for this project");
        }

        User raiser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProjectExit exit = ProjectExit.builder()
                .project(project)
                .raisedBy(raiser)
                .reason(request.reason().trim())
                .status(ProjectExitStatus.OPEN)
                .createdAt(Instant.now())
                .build();

        for (Milestone milestone : project.getMilestones()) {
            if (!HELD_MILESTONE_STATUSES.contains(milestone.getStatus())) {
                continue;
            }
            EscrowHold hold = escrowHoldRepository.findByMilestoneId(milestone.getId()).orElse(null);
            if (hold == null || hold.getStatus() != EscrowHoldStatus.HELD) {
                continue;
            }
            exit.getSettlements().add(ProjectExitSettlement.builder()
                    .projectExit(exit)
                    .milestone(milestone)
                    .holdAmount(hold.getAmount())
                    .build());
        }

        project.setStatus(ProjectStatus.EXIT_DISPUTED);
        projectRepository.save(project);
        ProjectExit saved = projectExitRepository.save(exit);

        Long otherPartyId = project.getClient().getId().equals(userId)
                ? project.getFreelancer().getId()
                : project.getClient().getId();

        List<Long> recipientIds = new ArrayList<>();
        recipientIds.add(otherPartyId);
        userRepository.findByRoleInWithCreatedBy(ADMIN_ROLES).stream()
                .map(User::getId)
                .filter(id -> !id.equals(userId) && !id.equals(otherPartyId))
                .forEach(recipientIds::add);

        notificationService.notifyMany(
                recipientIds,
                NotificationType.PROJECT_EXIT_RAISED,
                "Project exit requested",
                "An exit dispute was raised on project \"" + project.getTitle() + "\".",
                NotificationReferenceType.PROJECT,
                project.getId());

        log.info("Project exit raised: exitId={} projectId={} raisedBy={} settlements={}",
                saved.getId(), projectId, userId, saved.getSettlements().size());
        return toResponse(saved);
    }

    @Transactional
    public ProjectExitResponse resolve(Long exitId, ResolveProjectExitRequest request) {
        SecurityUtils.requireAdmin();
        Long adminId = SecurityUtils.getCurrentUserId();

        ProjectExit exit = projectExitRepository.findByIdWithDetails(exitId)
                .orElseThrow(() -> new ResourceNotFoundException("Project exit not found"));

        if (exit.getStatus() != ProjectExitStatus.OPEN) {
            throw new IllegalStateException("Project exit is already resolved");
        }

        Project project = exit.getProject();
        if (project.getStatus() != ProjectStatus.EXIT_DISPUTED) {
            throw new IllegalStateException("Project is not in EXIT_DISPUTED status");
        }

        // Capture freelancer before possible clear on REOPEN
        Long freelancerId = project.getFreelancer() != null ? project.getFreelancer().getId() : null;
        Long clientId = project.getClient().getId();

        List<ResolveProjectExitRequest.SettlementDecision> decisions =
                request.settlements() == null ? List.of() : request.settlements();

        applySettlements(exit, decisions, adminId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        ProjectExitOutcome outcome = request.projectOutcome();
        if (outcome == ProjectExitOutcome.CANCELLED) {
            project.setStatus(ProjectStatus.CANCELLED);
        } else {
            project.setFreelancer(null);
            project.setStatus(ProjectStatus.OPEN);
            projectApplicationRepository.deleteByProjectId(project.getId());
            projectAgreementService.deleteForProject(project.getId());
        }
        projectRepository.save(project);

        exit.setStatus(ProjectExitStatus.RESOLVED);
        exit.setProjectOutcome(outcome);
        exit.setAdminNote(blankToNull(request.adminNote()));
        exit.setResolvedBy(admin);
        exit.setResolvedAt(Instant.now());
        projectExitRepository.save(exit);

        List<Long> recipientIds = new ArrayList<>();
        recipientIds.add(clientId);
        if (freelancerId != null) {
            recipientIds.add(freelancerId);
        }

        String outcomeLabel = outcome == ProjectExitOutcome.CANCELLED ? "cancelled" : "reopened";
        notificationService.notifyMany(
                recipientIds,
                NotificationType.PROJECT_EXIT_RESOLVED,
                "Project exit resolved",
                "Exit dispute on project \"" + project.getTitle()
                        + "\" was resolved (project " + outcomeLabel + "). Admin decision is final.",
                NotificationReferenceType.PROJECT,
                project.getId());

        log.info("Project exit resolved: exitId={} outcome={} adminId={}", exitId, outcome, adminId);
        return toResponse(projectExitRepository.findByIdWithDetails(exitId).orElse(exit));
    }

    @Transactional(readOnly = true)
    public ProjectExitResponse getById(Long exitId) {
        ProjectExit exit = projectExitRepository.findByIdWithDetails(exitId)
                .orElseThrow(() -> new ResourceNotFoundException("Project exit not found"));
        assertCanView(exit);
        return toResponse(exit);
    }

    @Transactional(readOnly = true)
    public List<ProjectExitResponse> listForAdmin(ProjectExitStatus status) {
        SecurityUtils.requireAdmin();
        return projectExitRepository.findAllWithDetails(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectExitResponse getOpenForProject(Long projectId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        assertPartyOnProject(project, userId);

        return projectExitRepository.findByProjectIdAndStatus(projectId, ProjectExitStatus.OPEN)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No open project exit for this project"));
    }

    private void applySettlements(
            ProjectExit exit,
            List<ResolveProjectExitRequest.SettlementDecision> decisions,
            Long adminId) {
        List<ProjectExitSettlement> settlements = exit.getSettlements();

        if (settlements.isEmpty()) {
            if (!decisions.isEmpty()) {
                throw new IllegalArgumentException("No held milestones to settle for this exit");
            }
            return;
        }

        if (decisions.size() != settlements.size()) {
            throw new IllegalArgumentException(
                    "Provide a settlement decision for every held milestone ("
                            + settlements.size() + " required)");
        }

        Map<Long, BigDecimal> freelancerByMilestone = new HashMap<>();
        for (ResolveProjectExitRequest.SettlementDecision decision : decisions) {
            if (freelancerByMilestone.put(decision.milestoneId(), decision.freelancerAmount()) != null) {
                throw new IllegalArgumentException(
                        "Duplicate milestoneId in settlements: " + decision.milestoneId());
            }
        }

        Set<Long> expectedIds = new HashSet<>();
        for (ProjectExitSettlement settlement : settlements) {
            expectedIds.add(settlement.getMilestone().getId());
        }
        if (!freelancerByMilestone.keySet().equals(expectedIds)) {
            throw new IllegalArgumentException("Settlement milestoneIds must match the exit's held milestones");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        for (ProjectExitSettlement settlement : settlements) {
            Long milestoneId = settlement.getMilestone().getId();
            BigDecimal freelancerAmount = freelancerByMilestone.get(milestoneId)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal hold = settlement.getHoldAmount();

            if (freelancerAmount.compareTo(BigDecimal.ZERO) < 0 || freelancerAmount.compareTo(hold) > 0) {
                throw new IllegalArgumentException(
                        "freelancerAmount must be between 0 and holdAmount for milestoneId=" + milestoneId);
            }

            BigDecimal clientRefund = hold.subtract(freelancerAmount).setScale(4, RoundingMode.HALF_UP);

            disputeRepository.findByMilestoneId(milestoneId).ifPresent(dispute -> {
                if (dispute.getStatus() == DisputeStatus.OPEN) {
                    dispute.setStatus(DisputeStatus.RESOLVED);
                    dispute.setAdminNote("Superseded by project exit #" + exit.getId());
                    dispute.setResolvedAt(Instant.now());
                    dispute.setResolvedBy(admin);
                    disputeRepository.save(dispute);
                }
            });

            escrowService.settleHoldSplit(milestoneId, freelancerAmount, clientRefund);
            settlement.setFreelancerAmount(freelancerAmount);
            settlement.setClientRefundAmount(clientRefund);
        }
    }

    private void assertCanView(ProjectExit exit) {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        Project project = exit.getProject();
        assertPartyOnProject(project, userId);
    }

    private void assertPartyOnProject(Project project, Long userId) {
        boolean isClient = project.getClient().getId().equals(userId);
        boolean isFreelancer = project.getFreelancer() != null
                && project.getFreelancer().getId().equals(userId);
        if (!isClient && !isFreelancer) {
            throw new ForbiddenException("Only the project client or freelancer can access project exits");
        }
    }

    private void rejectAdminAccess() {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            throw new ForbiddenException("Admins cannot raise project exits");
        }
    }

    private ProjectExitResponse toResponse(ProjectExit exit) {
        Project project = exit.getProject();
        List<ProjectExitResponse.SettlementResponse> settlements = exit.getSettlements().stream()
                .map(s -> new ProjectExitResponse.SettlementResponse(
                        s.getId(),
                        s.getMilestone().getId(),
                        s.getMilestone().getTitle(),
                        s.getMilestone().getStatus(),
                        s.getHoldAmount(),
                        s.getFreelancerAmount(),
                        s.getClientRefundAmount()))
                .toList();

        return new ProjectExitResponse(
                exit.getId(),
                project.getId(),
                project.getTitle(),
                project.getStatus(),
                project.getClient().getId(),
                project.getClient().getName(),
                project.getFreelancer() != null ? project.getFreelancer().getId() : null,
                project.getFreelancer() != null ? project.getFreelancer().getName() : null,
                exit.getRaisedBy().getId(),
                exit.getRaisedBy().getName(),
                exit.getReason(),
                exit.getStatus(),
                exit.getProjectOutcome(),
                exit.getAdminNote(),
                exit.getResolvedBy() != null ? exit.getResolvedBy().getId() : null,
                exit.getResolvedBy() != null ? exit.getResolvedBy().getName() : null,
                exit.getCreatedAt(),
                exit.getResolvedAt(),
                settlements);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
