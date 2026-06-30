package com.escrowflow;

import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.security.UserPrincipal;
import com.escrowflow.service.AuthService;
import com.escrowflow.service.ProjectService;
import com.escrowflow.service.WalletConsistencyService;
import com.escrowflow.web.dto.CreateProjectRequest;
import com.escrowflow.web.dto.SignupRequest;
import com.escrowflow.web.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProjectIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletConsistencyService walletConsistencyService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void createAndAcceptProject_doesNotChangeWalletBalances() {
        authService.signup(new SignupRequest("Jane Client", "client@example.com", "password123", UserRole.CLIENT));
        authService.signup(new SignupRequest("Bob Freelancer", "freelancer@example.com", "password123", UserRole.FREELANCER));

        var client = userRepository.findByEmail("client@example.com").orElseThrow();
        var freelancer = userRepository.findByEmail("freelancer@example.com").orElseThrow();
        var clientWallet = walletRepository.findByUser_Id(client.getId()).orElseThrow();
        var freelancerWallet = walletRepository.findByUser_Id(freelancer.getId()).orElseThrow();
        var clientBalanceBefore = clientWallet.getBalance();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var created = projectService.create(new CreateProjectRequest(
                "Website redesign",
                "Three-phase delivery",
                List.of(
                        new CreateProjectRequest.CreateMilestoneRequest("Wireframes", "Figma files", new BigDecimal("5000")),
                        new CreateProjectRequest.CreateMilestoneRequest("Implementation", "React app", new BigDecimal("15000"))
                )));

        assertThat(created.status()).isEqualTo(ProjectStatus.OPEN);
        assertThat(created.freelancer()).isNull();
        assertThat(created.milestones()).hasSize(2);
        assertThat(created.milestones()).allMatch(m -> m.status() == MilestoneStatus.PENDING);

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        var openProjects = projectService.listForUser(null);
        assertThat(openProjects).extracting("id").contains(created.id());

        var accepted = projectService.accept(created.id());
        assertThat(accepted.status()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(accepted.freelancer().id()).isEqualTo(freelancer.getId());

        var clientWalletAfter = walletRepository.findById(clientWallet.getId()).orElseThrow();
        var freelancerWalletAfter = walletRepository.findById(freelancerWallet.getId()).orElseThrow();
        assertThat(clientWalletAfter.getBalance()).isEqualByComparingTo(clientBalanceBefore);
        assertThat(freelancerWalletAfter.getBalance()).isEqualByComparingTo(new BigDecimal("10000.0000"));
        assertThat(walletConsistencyService.isWalletConsistent(clientWallet.getId())).isTrue();
        assertThat(walletConsistencyService.isWalletConsistent(freelancerWallet.getId())).isTrue();

        var persisted = projectRepository.findByIdWithDetails(created.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(persisted.getFreelancer().getId()).isEqualTo(freelancer.getId());
    }

    @Test
    @Transactional
    void accept_ownProject_isForbidden() {
        authService.signup(new SignupRequest("Solo User", "both@example.com", "password123", UserRole.BOTH));
        var user = userRepository.findByEmail("both@example.com").orElseThrow();

        authenticate(user.getId(), user.getEmail(), UserRole.BOTH);
        var created = projectService.create(new CreateProjectRequest(
                "Solo project",
                null,
                List.of(new CreateProjectRequest.CreateMilestoneRequest("Phase 1", null, new BigDecimal("1000")))));

        assertThatThrownBy(() -> projectService.accept(created.id()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own project");
    }

    private void authenticate(Long userId, String email, UserRole role) {
        UserPrincipal principal = new UserPrincipal(userId, email, role);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
