package com.escrowflow;

import com.escrowflow.config.TestRedisConfig;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.MilestoneRepository;
import com.escrowflow.repository.ProjectRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.security.UserPrincipal;
import com.escrowflow.service.AuthService;
import com.escrowflow.service.EscrowService;
import com.escrowflow.service.MilestoneService;
import com.escrowflow.service.ProjectService;
import com.escrowflow.service.WalletConsistencyService;
import com.escrowflow.web.dto.CreateProjectRequest;
import com.escrowflow.web.dto.SignupRequest;
import com.escrowflow.web.exception.InvalidMilestoneStateException;
import com.escrowflow.web.exception.InsufficientBalanceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class EscrowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private EscrowHoldRepository escrowHoldRepository;

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
    void fullEscrowCycle_lockSubmitApprove_updatesWalletBalances() {
        authService.signup(new SignupRequest("Client", "client@test.com", "password123", UserRole.CLIENT));
        authService.signup(new SignupRequest("Freelancer", "freelancer@test.com", "password123", UserRole.FREELANCER));

        var client = userRepository.findByEmail("client@test.com").orElseThrow();
        var freelancer = userRepository.findByEmail("freelancer@test.com").orElseThrow();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var project = projectService.create(new CreateProjectRequest(
                "Test Project",
                "Description",
                List.of(new CreateProjectRequest.CreateMilestoneRequest("M1", "First", new BigDecimal("3000")))
        ));

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        var accepted = projectService.accept(project.id());

        var milestone = milestoneRepository.findById(accepted.milestones().get(0).id()).orElseThrow();
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.PENDING);

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var clientWalletBefore = walletRepository.findByUser_Id(client.getId()).orElseThrow();
        var freelancerWalletBefore = walletRepository.findByUser_Id(freelancer.getId()).orElseThrow();

        escrowService.lockFunds(milestone.getId(), client.getId());

        var clientWalletAfterLock = walletRepository.findByUser_Id(client.getId()).orElseThrow();
        assertThat(clientWalletAfterLock.getBalance())
                .isEqualByComparingTo(clientWalletBefore.getBalance().subtract(new BigDecimal("3000")));

        var milestoneAfterLock = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertThat(milestoneAfterLock.getStatus()).isEqualTo(MilestoneStatus.FUNDS_LOCKED);

        var hold = escrowHoldRepository.findByMilestoneId(milestone.getId()).orElseThrow();
        assertThat(hold.getStatus()).isEqualTo(EscrowHoldStatus.HELD);
        assertThat(hold.getAmount()).isEqualByComparingTo(new BigDecimal("3000"));

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        milestoneService.submit(milestone.getId(), freelancer.getId(), "Work completed");

        var milestoneAfterSubmit = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertThat(milestoneAfterSubmit.getStatus()).isEqualTo(MilestoneStatus.SUBMITTED);
        assertThat(milestoneAfterSubmit.getSubmittedNote()).isEqualTo("Work completed");

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        escrowService.approve(milestone.getId(), client.getId());

        var milestoneAfterApprove = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertThat(milestoneAfterApprove.getStatus()).isEqualTo(MilestoneStatus.APPROVED);

        var holdAfterApprove = escrowHoldRepository.findById(hold.getId()).orElseThrow();
        assertThat(holdAfterApprove.getStatus()).isEqualTo(EscrowHoldStatus.RELEASED);
        assertThat(holdAfterApprove.getResolvedAt()).isNotNull();

        var clientWalletFinal = walletRepository.findByUser_Id(client.getId()).orElseThrow();
        var freelancerWalletFinal = walletRepository.findByUser_Id(freelancer.getId()).orElseThrow();

        assertThat(clientWalletFinal.getBalance())
                .isEqualByComparingTo(clientWalletBefore.getBalance().subtract(new BigDecimal("3000")));
        assertThat(freelancerWalletFinal.getBalance())
                .isEqualByComparingTo(freelancerWalletBefore.getBalance().add(new BigDecimal("3000")));

        assertThat(walletConsistencyService.isWalletConsistent(client.getId())).isTrue();
        assertThat(walletConsistencyService.isWalletConsistent(freelancer.getId())).isTrue();
    }

    @Test
    @Transactional
    void disputeFlow_refundsClient() {
        authService.signup(new SignupRequest("Client2", "client2@test.com", "password123", UserRole.CLIENT));
        authService.signup(new SignupRequest("Freelancer2", "freelancer2@test.com", "password123", UserRole.FREELANCER));

        var client = userRepository.findByEmail("client2@test.com").orElseThrow();
        var freelancer = userRepository.findByEmail("freelancer2@test.com").orElseThrow();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var project = projectService.create(new CreateProjectRequest(
                "Dispute Test",
                "Description",
                List.of(new CreateProjectRequest.CreateMilestoneRequest("M1", "Task", new BigDecimal("2000")))
        ));

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        projectService.accept(project.id());

        var milestone = milestoneRepository.findById(project.milestones().get(0).id()).orElseThrow();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var clientBalanceBefore = walletRepository.findByUser_Id(client.getId()).orElseThrow().getBalance();

        escrowService.lockFunds(milestone.getId(), client.getId());

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        milestoneService.submit(milestone.getId(), freelancer.getId(), "Deliverable");

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        escrowService.dispute(milestone.getId(), client.getId(), "Does not meet requirements");

        var milestoneAfterDispute = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertThat(milestoneAfterDispute.getStatus()).isEqualTo(MilestoneStatus.REFUNDED);

        var hold = escrowHoldRepository.findByMilestoneId(milestone.getId()).orElseThrow();
        assertThat(hold.getStatus()).isEqualTo(EscrowHoldStatus.REFUNDED);

        var clientBalanceAfter = walletRepository.findByUser_Id(client.getId()).orElseThrow().getBalance();
        assertThat(clientBalanceAfter).isEqualByComparingTo(clientBalanceBefore);

        assertThat(walletConsistencyService.isWalletConsistent(client.getId())).isTrue();
    }

    @Test
    @Transactional
    void disputeRateLimit_blocksSixthDisputeWithinTwentyFourHours() {
        authService.signup(new SignupRequest("RateLimitClient", "ratelimit-client@test.com", "password123", UserRole.CLIENT));
        authService.signup(new SignupRequest("RateLimitFreelancer", "ratelimit-freelancer@test.com", "password123", UserRole.FREELANCER));

        var client = userRepository.findByEmail("ratelimit-client@test.com").orElseThrow();
        var freelancer = userRepository.findByEmail("ratelimit-freelancer@test.com").orElseThrow();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var project = projectService.create(new CreateProjectRequest(
                "Rate Limit Test Project",
                "Six milestones to exercise the 5-per-day dispute limit",
                List.of(
                        new CreateProjectRequest.CreateMilestoneRequest("M1", "Task 1", new BigDecimal("100")),
                        new CreateProjectRequest.CreateMilestoneRequest("M2", "Task 2", new BigDecimal("100")),
                        new CreateProjectRequest.CreateMilestoneRequest("M3", "Task 3", new BigDecimal("100")),
                        new CreateProjectRequest.CreateMilestoneRequest("M4", "Task 4", new BigDecimal("100")),
                        new CreateProjectRequest.CreateMilestoneRequest("M5", "Task 5", new BigDecimal("100")),
                        new CreateProjectRequest.CreateMilestoneRequest("M6", "Task 6", new BigDecimal("100"))
                )
        ));

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        projectService.accept(project.id());

        List<Long> milestoneIds = project.milestones().stream()
                .map(com.escrowflow.web.dto.MilestoneResponse::id)
                .toList();

        for (int i = 0; i < 5; i++) {
            Long milestoneId = milestoneIds.get(i);

            authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
            escrowService.lockFunds(milestoneId, client.getId());

            authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
            milestoneService.submit(milestoneId, freelancer.getId(), "Work submitted " + i);

            authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
            escrowService.dispute(milestoneId, client.getId(), "Reason " + i);

            var milestoneAfter = milestoneRepository.findById(milestoneId).orElseThrow();
            assertThat(milestoneAfter.getStatus()).isEqualTo(MilestoneStatus.REFUNDED);
        }

        Long sixthMilestoneId = milestoneIds.get(5);

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        escrowService.lockFunds(sixthMilestoneId, client.getId());

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        milestoneService.submit(sixthMilestoneId, freelancer.getId(), "Work submitted 6");

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        assertThatThrownBy(() -> escrowService.dispute(sixthMilestoneId, client.getId(), "Should be blocked"))
                .isInstanceOf(com.escrowflow.web.exception.RateLimitExceededException.class)
                .hasMessageContaining("Dispute rate limit exceeded");

        var sixthMilestoneAfter = milestoneRepository.findById(sixthMilestoneId).orElseThrow();
        assertThat(sixthMilestoneAfter.getStatus()).isEqualTo(MilestoneStatus.SUBMITTED);
    }

    @Test
    @Transactional
    void lockFunds_invalidTransitions_throwsException() {
        authService.signup(new SignupRequest("Client3", "client3@test.com", "password123", UserRole.CLIENT));

        var client = userRepository.findByEmail("client3@test.com").orElseThrow();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var project = projectService.create(new CreateProjectRequest(
                "State Test",
                "Description",
                List.of(new CreateProjectRequest.CreateMilestoneRequest("M1", "Task", new BigDecimal("1000")))
        ));

        var milestone = milestoneRepository.findById(project.milestones().get(0).id()).orElseThrow();

        escrowService.lockFunds(milestone.getId(), client.getId());

        assertThatThrownBy(() -> escrowService.lockFunds(milestone.getId(), client.getId()))
                .isInstanceOf(InvalidMilestoneStateException.class)
                .hasMessageContaining("FUNDS_LOCKED");
    }

    @Test
    @Transactional
    void lockFunds_insufficientBalance_throwsException() {
        authService.signup(new SignupRequest("PoorClient", "poor@test.com", "password123", UserRole.CLIENT));

        var client = userRepository.findByEmail("poor@test.com").orElseThrow();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var project = projectService.create(new CreateProjectRequest(
                "Expensive Project",
                "Description",
                List.of(new CreateProjectRequest.CreateMilestoneRequest("M1", "Task", new BigDecimal("50000")))
        ));

        var milestone = milestoneRepository.findById(project.milestones().get(0).id()).orElseThrow();

        assertThatThrownBy(() -> escrowService.lockFunds(milestone.getId(), client.getId()))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void parallelLockAttempts_onlyOneSucceeds() throws InterruptedException {
        authService.signup(new SignupRequest("ConcurrentClient", "concurrent@test.com", "password123", UserRole.CLIENT));
        authService.signup(new SignupRequest("ConcurrentFreelancer", "confreelancer@test.com", "password123", UserRole.FREELANCER));

        var client = userRepository.findByEmail("concurrent@test.com").orElseThrow();
        var freelancer = userRepository.findByEmail("confreelancer@test.com").orElseThrow();

        authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
        var project = projectService.create(new CreateProjectRequest(
                "Concurrent Test",
                null,
                List.of(
                        new CreateProjectRequest.CreateMilestoneRequest("M1", "Task1", new BigDecimal("2000")),
                        new CreateProjectRequest.CreateMilestoneRequest("M2", "Task2", new BigDecimal("2000"))
                )
        ));

        authenticate(freelancer.getId(), freelancer.getEmail(), UserRole.FREELANCER);
        projectService.accept(project.id());

        var milestone1 = project.milestones().get(0).id();
        var milestone2 = project.milestones().get(1).id();

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    authenticate(client.getId(), client.getEmail(), UserRole.CLIENT);
                    Long milestoneId = (index % 2 == 0) ? milestone1 : milestone2;
                    escrowService.lockFunds(milestoneId, client.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                    SecurityContextHolder.clearContext();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(2);

        var m1 = milestoneRepository.findById(milestone1).orElseThrow();
        var m2 = milestoneRepository.findById(milestone2).orElseThrow();
        assertThat(m1.getStatus()).isEqualTo(MilestoneStatus.FUNDS_LOCKED);
        assertThat(m2.getStatus()).isEqualTo(MilestoneStatus.FUNDS_LOCKED);

        var clientWallet = walletRepository.findByUser_Id(client.getId()).orElseThrow();
        assertThat(clientWallet.getBalance()).isEqualByComparingTo(new BigDecimal("6000.0000"));
        assertThat(walletConsistencyService.isWalletConsistent(client.getId())).isTrue();
    }

    private void authenticate(Long userId, String email, UserRole role) {
        UserPrincipal principal = new UserPrincipal(userId, email, role);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
