package com.escrowflow;

import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.security.UserPrincipal;
import com.escrowflow.service.AuthService;
import com.escrowflow.service.EscrowService;
import com.escrowflow.service.MilestoneService;
import com.escrowflow.service.ProjectService;
import com.escrowflow.service.WalletConsistencyService;
import com.escrowflow.service.WalletService;
import com.escrowflow.web.dto.CreateProjectRequest;
import com.escrowflow.web.dto.SignupRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest
@ActiveProfiles("test")
class WalletConsistencyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private WalletConsistencyService walletConsistencyService;

    @Autowired
    private UserRepository userRepository;

    private User client;
    private User freelancer;

    @BeforeEach
    @Transactional
    void setUp() {
        authService.signup(new SignupRequest(
                "Client User",
                "consistency-client@example.com",
                "password123",
                UserRole.CLIENT));

        authService.signup(new SignupRequest(
                "Freelancer User",
                "consistency-freelancer@example.com",
                "password123",
                UserRole.FREELANCER));

        client = userRepository.findByEmail("consistency-client@example.com").orElseThrow();
        freelancer = userRepository.findByEmail("consistency-freelancer@example.com").orElseThrow();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void allWallets_remainConsistent_afterCompleteEscrowCycle() {
        walletService.addFunds(client.getId(), new BigDecimal("5000.00"));

        CreateProjectRequest request = new CreateProjectRequest(
                "Test Project",
                "Test Description",
                List.of(
                        new CreateProjectRequest.CreateMilestoneRequest(
                                "Milestone 1", "Description 1", new BigDecimal("2000.00")),
                        new CreateProjectRequest.CreateMilestoneRequest(
                                "Milestone 2", "Description 2", new BigDecimal("3000.00"))
                )
        );

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var project = projectService.create(request);

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        projectService.accept(project.id());

        var m1Id = project.milestones().get(0).id();
        var m2Id = project.milestones().get(1).id();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        escrowService.lockFunds(m1Id, client.getId());

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        milestoneService.submit(m1Id, freelancer.getId(), "Work submitted");

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        escrowService.approve(m1Id, client.getId());

        assertThat(walletConsistencyService.areAllWalletsConsistent()).isTrue();
        assertThat(walletConsistencyService.findInconsistentWalletIds()).isEmpty();

        escrowService.lockFunds(m2Id, client.getId());

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        milestoneService.submit(m2Id, freelancer.getId(), "Work submitted");

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        escrowService.dispute(m2Id, client.getId(), "Not satisfied");

        assertThat(walletConsistencyService.areAllWalletsConsistent()).isTrue();
        assertThat(walletConsistencyService.findInconsistentWalletIds()).isEmpty();
    }

    @Test
    @Transactional
    void allWallets_remainConsistent_afterMultipleFundOperations() {
        walletService.addFunds(client.getId(), new BigDecimal("1000.00"));
        walletService.addFunds(client.getId(), new BigDecimal("2000.00"));
        walletService.addFunds(freelancer.getId(), new BigDecimal("500.00"));

        assertThat(walletConsistencyService.areAllWalletsConsistent()).isTrue();
        assertThat(walletConsistencyService.findInconsistentWalletIds()).isEmpty();
    }

    @Test
    @Transactional
    void newlyCreatedWallets_areConsistent() {
        assertThat(walletConsistencyService.isWalletConsistent(
                walletService.findWalletByUserId(client.getId()).getId())).isTrue();
        assertThat(walletConsistencyService.isWalletConsistent(
                walletService.findWalletByUserId(freelancer.getId()).getId())).isTrue();
        assertThat(walletConsistencyService.areAllWalletsConsistent()).isTrue();
    }

    private void authenticate(Long userId, String email, UserRole role) {
        UserPrincipal principal = new UserPrincipal(userId, email, role);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
