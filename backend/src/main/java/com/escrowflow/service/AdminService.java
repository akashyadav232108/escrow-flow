package com.escrowflow.service;

import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.MilestoneRepository;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.AdminDashboardStatsResponse;
import com.escrowflow.web.dto.AdminUserResponse;
import com.escrowflow.web.dto.CreateAdminRequest;
import com.escrowflow.web.exception.EmailAlreadyExistsException;
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
    private final MilestoneRepository milestoneRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            EscrowHoldRepository escrowHoldRepository,
            MilestoneRepository milestoneRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.milestoneRepository = milestoneRepository;
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
                milestoneRepository.countByStatus(MilestoneStatus.DISPUTED));
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
}
