package com.escrowflow.service;

import com.escrowflow.domain.Dispute;
import com.escrowflow.domain.EscrowHold;
import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.DisputeStatus;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.DisputeRepository;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.AdminDashboardStatsResponse;
import com.escrowflow.web.dto.AdminUserResponse;
import com.escrowflow.web.dto.CreateAdminRequest;
import com.escrowflow.web.dto.DisputeResponse;
import com.escrowflow.web.dto.ResolveDisputeRequest;
import com.escrowflow.web.exception.EmailAlreadyExistsException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@Slf4j
public class AdminService {

    private static final EnumSet<UserRole> ADMIN_ROLES = EnumSet.of(UserRole.ADMIN, UserRole.SUPER_ADMIN);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final DisputeRepository disputeRepository;
    private final EscrowService escrowService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            EscrowHoldRepository escrowHoldRepository,
            DisputeRepository disputeRepository,
            EscrowService escrowService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.disputeRepository = disputeRepository;
        this.escrowService = escrowService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminUserResponse createAdmin(CreateAdminRequest request) {
        SecurityUtils.requireSuperAdmin();

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User creator = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated super admin not found"));

        User admin = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.ADMIN)
                .createdBy(creator)
                .build());

        // Admins do not get a wallet — they only resolve escrow via dispute endpoints.
        log.info("Admin created: adminId={} email={} createdBy={}",
                admin.getId(), admin.getEmail(), creator.getId());

        return toAdminUserResponse(admin);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listAdmins() {
        SecurityUtils.requireAdmin();
        return userRepository.findByRoleInWithCreatedBy(ADMIN_ROLES).stream()
                .map(this::toAdminUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        SecurityUtils.requireAdmin();

        long clients = userRepository.countByRole(UserRole.CLIENT);
        long freelancers = userRepository.countByRole(UserRole.FREELANCER);
        long both = userRepository.countByRole(UserRole.BOTH);
        long admins = userRepository.countByRole(UserRole.ADMIN)
                + userRepository.countByRole(UserRole.SUPER_ADMIN);

        return new AdminDashboardStatsResponse(
                clients + freelancers + both + admins,
                clients,
                freelancers,
                both,
                admins,
                projectRepository.countByStatus(ProjectStatus.OPEN),
                projectRepository.countByStatus(ProjectStatus.IN_PROGRESS),
                projectRepository.countByStatus(ProjectStatus.COMPLETED),
                projectRepository.countByStatus(ProjectStatus.CANCELLED),
                escrowHoldRepository.sumAmountByStatus(EscrowHoldStatus.HELD),
                disputeRepository.countByStatus(DisputeStatus.OPEN));
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> listDisputes(DisputeStatus status) {
        SecurityUtils.requireAdmin();
        return disputeRepository.findAllWithDetails(status).stream()
                .map(this::toDisputeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(Long disputeId) {
        SecurityUtils.requireAdmin();
        Dispute dispute = disputeRepository.findByIdWithDetails(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        return toDisputeResponse(dispute);
    }

    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, ResolveDisputeRequest request) {
        SecurityUtils.requireAdmin();
        escrowService.resolveDispute(
                disputeId,
                SecurityUtils.getCurrentUserId(),
                request.decision(),
                request.note());
        return getDispute(disputeId);
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        User createdBy = user.getCreatedBy();
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getName() : null);
    }

    private DisputeResponse toDisputeResponse(Dispute dispute) {
        Milestone milestone = dispute.getMilestone();
        Project project = milestone.getProject();
        User raisedBy = dispute.getRaisedBy();
        User resolvedBy = dispute.getResolvedBy();
        User freelancer = project.getFreelancer();

        EscrowHoldStatus holdStatus = escrowHoldRepository.findByMilestoneId(milestone.getId())
                .map(EscrowHold::getStatus)
                .orElse(null);

        return new DisputeResponse(
                dispute.getId(),
                milestone.getId(),
                milestone.getTitle(),
                milestone.getAmount(),
                milestone.getStatus(),
                holdStatus,
                project.getId(),
                project.getTitle(),
                project.getClient().getId(),
                project.getClient().getName(),
                freelancer != null ? freelancer.getId() : null,
                freelancer != null ? freelancer.getName() : null,
                raisedBy.getId(),
                raisedBy.getName(),
                dispute.getReason(),
                dispute.getStatus(),
                dispute.getResolution(),
                resolvedBy != null ? resolvedBy.getId() : null,
                resolvedBy != null ? resolvedBy.getName() : null,
                dispute.getAdminNote(),
                milestone.getSubmittedNote(),
                dispute.getCreatedAt(),
                dispute.getResolvedAt());
    }
}
