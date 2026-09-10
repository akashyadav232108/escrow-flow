package com.escrowflow.service;

import com.escrowflow.domain.Dispute;
import com.escrowflow.domain.EscrowHold;
import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.User;
import com.escrowflow.domain.UserWarning;
import com.escrowflow.domain.enums.AccountStatus;
import com.escrowflow.domain.enums.DisputeStatus;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.DisputeRepository;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.repository.UserWarningRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.AdminDashboardStatsResponse;
import com.escrowflow.web.dto.AdminUserResponse;
import com.escrowflow.web.dto.CreateAdminRequest;
import com.escrowflow.web.dto.DisputeResponse;
import com.escrowflow.web.dto.ManagedUserResponse;
import com.escrowflow.web.dto.ModerationReasonRequest;
import com.escrowflow.web.dto.ResolveDisputeRequest;
import com.escrowflow.web.dto.UserWarningResponse;
import com.escrowflow.web.exception.EmailAlreadyExistsException;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
@Slf4j
public class AdminService {

    private static final EnumSet<UserRole> ADMIN_ROLES = EnumSet.of(UserRole.ADMIN, UserRole.SUPER_ADMIN);
    private static final EnumSet<UserRole> MARKETPLACE_ROLES =
            EnumSet.of(UserRole.CLIENT, UserRole.FREELANCER, UserRole.BOTH);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final DisputeRepository disputeRepository;
    private final UserWarningRepository userWarningRepository;
    private final EscrowService escrowService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            EscrowHoldRepository escrowHoldRepository,
            DisputeRepository disputeRepository,
            UserWarningRepository userWarningRepository,
            EscrowService escrowService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.disputeRepository = disputeRepository;
        this.userWarningRepository = userWarningRepository;
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
                .accountStatus(AccountStatus.ACTIVE)
                .createdBy(creator)
                .build());

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
                userRepository.countByAccountStatus(AccountStatus.WARNED),
                userRepository.countByAccountStatus(AccountStatus.SUSPENDED),
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

    @Transactional(readOnly = true)
    public List<ManagedUserResponse> listUsers(AccountStatus status) {
        SecurityUtils.requireAdmin();
        return userRepository.findMarketplaceUsers(MARKETPLACE_ROLES, status).stream()
                .filter(u -> status != null || u.getAccountStatus() != AccountStatus.DELETED)
                .map(u -> toManagedUserResponse(u, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ManagedUserResponse getUser(Long userId) {
        SecurityUtils.requireAdmin();
        User user = requireMarketplaceUser(userId);
        return toManagedUserResponse(user, true);
    }

    @Transactional
    public ManagedUserResponse warnUser(Long userId, ModerationReasonRequest request) {
        SecurityUtils.requireAdmin();
        User user = requireMarketplaceUser(userId);
        ensureNotSelf(user);
        ensureNotDeleted(user);

        User admin = currentAdmin();
        userWarningRepository.save(UserWarning.builder()
                .user(user)
                .issuedBy(admin)
                .reason(request.reason())
                .build());

        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            user.setAccountStatus(AccountStatus.WARNED);
            userRepository.save(user);
        }

        log.info("User warned: userId={} adminId={}", userId, admin.getId());
        return toManagedUserResponse(user, true);
    }

    @Transactional
    public ManagedUserResponse suspendUser(Long userId, ModerationReasonRequest request) {
        SecurityUtils.requireAdmin();
        User user = requireMarketplaceUser(userId);
        ensureNotSelf(user);
        ensureNotDeleted(user);

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalStateException("User is already suspended");
        }

        User admin = currentAdmin();
        // Keep an audit trail of the suspend reason as a warning row.
        userWarningRepository.save(UserWarning.builder()
                .user(user)
                .issuedBy(admin)
                .reason("SUSPENDED: " + request.reason())
                .build());

        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);

        log.info("User suspended: userId={} adminId={}", userId, admin.getId());
        return toManagedUserResponse(user, true);
    }

    @Transactional
    public ManagedUserResponse unsuspendUser(Long userId) {
        SecurityUtils.requireAdmin();
        User user = requireMarketplaceUser(userId);
        ensureNotSelf(user);
        ensureNotDeleted(user);

        if (user.getAccountStatus() != AccountStatus.SUSPENDED) {
            throw new IllegalStateException("User is not suspended");
        }

        long warnings = userWarningRepository.countByUser_Id(userId);
        user.setAccountStatus(warnings > 0 ? AccountStatus.WARNED : AccountStatus.ACTIVE);
        userRepository.save(user);

        log.info("User unsuspended: userId={} newStatus={}", userId, user.getAccountStatus());
        return toManagedUserResponse(user, true);
    }

    @Transactional
    public ManagedUserResponse softDeleteUser(Long userId, ModerationReasonRequest request) {
        SecurityUtils.requireAdmin();
        User user = requireMarketplaceUser(userId);
        ensureNotSelf(user);
        ensureNotDeleted(user);

        long openDisputes = disputeRepository.countOpenDisputesForUser(userId);
        if (openDisputes > 0) {
            throw new IllegalStateException(
                    "Cannot delete user with open disputes (" + openDisputes + ")");
        }

        long heldEscrow = escrowHoldRepository.countHeldEscrowForClient(userId);
        if (heldEscrow > 0) {
            throw new IllegalStateException(
                    "Cannot delete user with active escrow holds (" + heldEscrow + ")");
        }

        long inProgressAsClient =
                projectRepository.countByClient_IdAndStatus(userId, ProjectStatus.IN_PROGRESS);
        long inProgressAsFreelancer =
                projectRepository.countByFreelancer_IdAndStatus(userId, ProjectStatus.IN_PROGRESS);
        if (inProgressAsClient + inProgressAsFreelancer > 0) {
            throw new IllegalStateException("Cannot delete user with in-progress projects");
        }

        User admin = currentAdmin();
        userWarningRepository.save(UserWarning.builder()
                .user(user)
                .issuedBy(admin)
                .reason("DELETED: " + request.reason())
                .build());

        user.setAccountStatus(AccountStatus.DELETED);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        log.info("User soft-deleted: userId={} adminId={}", userId, admin.getId());
        return toManagedUserResponse(user, true);
    }

    private User requireMarketplaceUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!MARKETPLACE_ROLES.contains(user.getRole())) {
            throw new ForbiddenException("Cannot moderate admin accounts via user moderation APIs");
        }
        return user;
    }

    private void ensureNotSelf(User user) {
        if (user.getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("Cannot moderate your own account");
        }
    }

    private void ensureNotDeleted(User user) {
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new IllegalStateException("User is already deleted");
        }
    }

    private User currentAdmin() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated admin not found"));
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

    private ManagedUserResponse toManagedUserResponse(User user, boolean includeWarnings) {
        long warningCount = userWarningRepository.countByUser_Id(user.getId());
        List<UserWarningResponse> warnings = includeWarnings
                ? userWarningRepository.findByUserIdWithIssuer(user.getId()).stream()
                        .map(this::toWarningResponse)
                        .toList()
                : List.of();

        return new ManagedUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                user.getDeletedAt(),
                warningCount,
                warnings);
    }

    private UserWarningResponse toWarningResponse(UserWarning warning) {
        return new UserWarningResponse(
                warning.getId(),
                warning.getReason(),
                warning.getIssuedBy().getId(),
                warning.getIssuedBy().getName(),
                warning.getCreatedAt());
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
