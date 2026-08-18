package com.escrowflow.service;

import com.escrowflow.domain.Dispute;
import com.escrowflow.domain.EscrowHold;
import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.User;
import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.enums.DisputeResolution;
import com.escrowflow.domain.enums.DisputeStatus;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.NotificationReferenceType;
import com.escrowflow.domain.enums.NotificationType;
import com.escrowflow.domain.enums.ProjectStatus;
import com.escrowflow.domain.enums.ReferenceType;
import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.infrastructure.DisputeRateLimitService;
import com.escrowflow.infrastructure.RedisWalletLockService;
import com.escrowflow.repository.DisputeRepository;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.MilestoneRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.InvalidMilestoneStateException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@Slf4j
public class EscrowService {

    private static final EnumSet<UserRole> ADMIN_ROLES = EnumSet.of(UserRole.ADMIN, UserRole.SUPER_ADMIN);

    private final MilestoneRepository milestoneRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final DisputeRepository disputeRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final RedisWalletLockService lockService;
    private final DisputeRateLimitService disputeRateLimitService;
    private final NotificationService notificationService;
    private final ProjectAgreementService projectAgreementService;

    public EscrowService(
            MilestoneRepository milestoneRepository,
            EscrowHoldRepository escrowHoldRepository,
            DisputeRepository disputeRepository,
            UserRepository userRepository,
            WalletService walletService,
            RedisWalletLockService lockService,
            DisputeRateLimitService disputeRateLimitService,
            NotificationService notificationService,
            ProjectAgreementService projectAgreementService) {
        this.milestoneRepository = milestoneRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.disputeRepository = disputeRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.lockService = lockService;
        this.disputeRateLimitService = disputeRateLimitService;
        this.notificationService = notificationService;
        this.projectAgreementService = projectAgreementService;
    }

    public void lockFunds(Long milestoneId, Long clientUserId) {
        Milestone milestone = milestoneRepository.findByIdWithProject(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        Project project = milestone.getProject();

        if (!project.getClient().getId().equals(clientUserId)) {
            throw new ForbiddenException("Only the project client can lock funds");
        }

        if (project.getStatus() == ProjectStatus.EXIT_DISPUTED) {
            throw new IllegalStateException("Cannot lock funds while project exit is under admin review");
        }

        projectAgreementService.requireFullyAccepted(project.getId());

        boolean initialLock = milestone.getStatus() == MilestoneStatus.PENDING;
        boolean relockAfterRefund = milestone.getStatus() == MilestoneStatus.REFUNDED;
        if (!initialLock && !relockAfterRefund) {
            throw new InvalidMilestoneStateException(
                    "Cannot lock funds for milestone in status: " + milestone.getStatus());
        }
        if (relockAfterRefund) {
            if (project.getStatus() != ProjectStatus.IN_PROGRESS || project.getFreelancer() == null) {
                throw new IllegalStateException(
                        "Can only re-lock a refunded milestone on an in-progress project with an assigned freelancer");
            }
        }

        Wallet clientWallet = walletService.findWalletByUserId(clientUserId);
        String lockRequestId = lockService.acquireLock(clientWallet.getId());

        try {
            lockFundsTransactional(milestone, clientWallet, relockAfterRefund);
            log.info("Funds locked: milestoneId={} amount={} clientWalletId={} relock={}",
                    milestoneId, milestone.getAmount(), clientWallet.getId(), relockAfterRefund);
        } finally {
            lockService.releaseLock(clientWallet.getId(), lockRequestId);
        }
    }

    @Transactional
    protected void lockFundsTransactional(Milestone milestone, Wallet clientWallet, boolean relockAfterRefund) {
        if (relockAfterRefund) {
            EscrowHold hold = escrowHoldRepository.findByMilestoneId(milestone.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));
            if (hold.getStatus() != EscrowHoldStatus.REFUNDED) {
                throw new InvalidMilestoneStateException(
                        "Cannot re-lock escrow hold in status: " + hold.getStatus());
            }

            walletService.debit(
                    clientWallet, milestone.getAmount(), ReferenceType.ESCROW_LOCK, hold.getId());

            hold.setAmount(milestone.getAmount());
            hold.setStatus(EscrowHoldStatus.HELD);
            hold.setResolvedAt(null);
            hold.setClientWallet(clientWallet);
            escrowHoldRepository.save(hold);
        } else {
            walletService.debit(clientWallet, milestone.getAmount(), ReferenceType.ESCROW_LOCK, null);

            EscrowHold hold = EscrowHold.builder()
                    .milestone(milestone)
                    .amount(milestone.getAmount())
                    .clientWallet(clientWallet)
                    .status(EscrowHoldStatus.HELD)
                    .build();
            escrowHoldRepository.save(hold);
        }

        milestone.setStatus(MilestoneStatus.FUNDS_LOCKED);
        milestone.setUpdatedAt(Instant.now());
        milestoneRepository.save(milestone);
    }

    @Transactional
    public void approve(Long milestoneId, Long clientUserId) {
        Milestone milestone = milestoneRepository.findByIdWithProject(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        Project project = milestone.getProject();

        if (!project.getClient().getId().equals(clientUserId)) {
            throw new ForbiddenException("Only the project client can approve milestone");
        }

        if (project.getStatus() == ProjectStatus.EXIT_DISPUTED) {
            throw new IllegalStateException("Cannot approve while project exit is under admin review");
        }

        projectAgreementService.requireFullyAccepted(project.getId());

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new InvalidMilestoneStateException(
                    "Cannot approve milestone in status: " + milestone.getStatus());
        }

        releaseToFreelancer(milestone);
        log.info("Milestone approved by client: milestoneId={}", milestoneId);
    }

    /**
     * Raises a dispute: milestone → DISPUTED, escrow stays HELD (no money movement).
     * Client or assigned freelancer may raise. Admin resolves later.
     */
    @Transactional
    public void dispute(Long milestoneId, Long raiserUserId, String reason) {
        disputeRateLimitService.checkAndIncrement(raiserUserId);

        Milestone milestone = milestoneRepository.findByIdWithProject(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        Project project = milestone.getProject();
        boolean isClient = project.getClient().getId().equals(raiserUserId);
        boolean isFreelancer = project.getFreelancer() != null
                && project.getFreelancer().getId().equals(raiserUserId);

        if (!isClient && !isFreelancer) {
            throw new ForbiddenException("Only the project client or freelancer can dispute milestone");
        }

        if (project.getStatus() == ProjectStatus.EXIT_DISPUTED) {
            throw new IllegalStateException("Cannot raise milestone dispute while project exit is open");
        }

        projectAgreementService.requireFullyAccepted(project.getId());

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new InvalidMilestoneStateException(
                    "Cannot dispute milestone in status: " + milestone.getStatus());
        }

        EscrowHold hold = escrowHoldRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));

        if (hold.getStatus() != EscrowHoldStatus.HELD) {
            throw new InvalidMilestoneStateException("Escrow hold is not in HELD status");
        }

        if (disputeRepository.existsByMilestoneId(milestoneId)) {
            throw new InvalidMilestoneStateException("A dispute already exists for this milestone");
        }

        User raiser = userRepository.findById(raiserUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Dispute dispute = disputeRepository.save(Dispute.builder()
                .milestone(milestone)
                .raisedBy(raiser)
                .reason(reason)
                .status(DisputeStatus.OPEN)
                .build());

        milestone.setStatus(MilestoneStatus.DISPUTED);
        milestone.setUpdatedAt(Instant.now());
        milestoneRepository.save(milestone);

        Long otherPartyId = isClient
                ? project.getFreelancer().getId()
                : project.getClient().getId();

        List<Long> recipientIds = new ArrayList<>();
        recipientIds.add(otherPartyId);
        userRepository.findByRoleInWithCreatedBy(ADMIN_ROLES).stream()
                .map(User::getId)
                .filter(id -> !id.equals(raiserUserId) && !id.equals(otherPartyId))
                .forEach(recipientIds::add);

        notificationService.notifyMany(
                recipientIds,
                NotificationType.DISPUTE_RAISED,
                "Dispute raised",
                "A dispute was raised on milestone \"" + milestone.getTitle()
                        + "\" for project \"" + project.getTitle() + "\".",
                NotificationReferenceType.DISPUTE,
                dispute.getId());

        log.info("Milestone disputed (funds frozen): milestoneId={} amount={} raisedBy={} reason={}",
                milestoneId, hold.getAmount(), raiserUserId, reason);
    }

    /**
     * Admin resolution: FREELANCER_WINS → release escrow; CLIENT_WINS → refund client.
     */
    @Transactional
    public void resolveDispute(Long disputeId, Long adminUserId, DisputeResolution decision, String adminNote) {
        Dispute dispute = disputeRepository.findByIdWithDetails(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new InvalidMilestoneStateException("Dispute is already resolved");
        }

        Milestone milestone = dispute.getMilestone();
        if (milestone.getStatus() != MilestoneStatus.DISPUTED) {
            throw new InvalidMilestoneStateException(
                    "Cannot resolve dispute for milestone in status: " + milestone.getStatus());
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (decision == DisputeResolution.FREELANCER_WINS) {
            releaseToFreelancer(milestone);
        } else {
            refundToClient(milestone);
        }

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(decision);
        dispute.setResolvedBy(admin);
        dispute.setAdminNote(adminNote);
        dispute.setResolvedAt(Instant.now());
        disputeRepository.save(dispute);

        Project project = milestone.getProject();
        List<Long> recipientIds = new ArrayList<>();
        recipientIds.add(project.getClient().getId());
        if (project.getFreelancer() != null) {
            recipientIds.add(project.getFreelancer().getId());
        }

        String outcome = decision == DisputeResolution.FREELANCER_WINS
                ? "freelancer wins"
                : "client wins";
        notificationService.notifyMany(
                recipientIds,
                NotificationType.DISPUTE_RESOLVED,
                "Dispute resolved",
                "Dispute on milestone \"" + milestone.getTitle()
                        + "\" for project \"" + project.getTitle()
                        + "\" was resolved (" + outcome + ").",
                NotificationReferenceType.DISPUTE,
                dispute.getId());

        log.info("Dispute resolved: disputeId={} decision={} adminId={}",
                disputeId, decision, adminUserId);
    }

    private void releaseToFreelancer(Milestone milestone) {
        Project project = milestone.getProject();
        Long milestoneId = milestone.getId();

        EscrowHold hold = escrowHoldRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));

        if (hold.getStatus() != EscrowHoldStatus.HELD) {
            throw new InvalidMilestoneStateException("Escrow hold is not in HELD status");
        }

        if (project.getFreelancer() == null) {
            throw new IllegalStateException("Project has no assigned freelancer");
        }

        Wallet freelancerWallet = walletService.findWalletByUserId(project.getFreelancer().getId());

        hold.setStatus(EscrowHoldStatus.RELEASED);
        hold.setResolvedAt(Instant.now());
        escrowHoldRepository.save(hold);

        walletService.credit(freelancerWallet, hold.getAmount(), ReferenceType.ESCROW_RELEASE, hold.getId());

        milestone.setStatus(MilestoneStatus.APPROVED);
        milestone.setUpdatedAt(Instant.now());
        milestoneRepository.save(milestone);

        log.info("Escrow released to freelancer: milestoneId={} amount={} freelancerWalletId={}",
                milestoneId, hold.getAmount(), freelancerWallet.getId());
    }

    private void refundToClient(Milestone milestone) {
        Long milestoneId = milestone.getId();

        EscrowHold hold = escrowHoldRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));

        if (hold.getStatus() != EscrowHoldStatus.HELD) {
            throw new InvalidMilestoneStateException("Escrow hold is not in HELD status");
        }

        hold.setStatus(EscrowHoldStatus.REFUNDED);
        hold.setResolvedAt(Instant.now());
        escrowHoldRepository.save(hold);

        walletService.credit(hold.getClientWallet(), hold.getAmount(), ReferenceType.ESCROW_REFUND, hold.getId());

        milestone.setStatus(MilestoneStatus.REFUNDED);
        milestone.setUpdatedAt(Instant.now());
        milestoneRepository.save(milestone);

        log.info("Escrow refunded to client: milestoneId={} amount={}", milestoneId, hold.getAmount());
    }

    /**
     * Partial settlement for project exit: credit freelancer and/or client from a HELD hold.
     * Amounts must sum to the hold amount.
     */
    @Transactional
    public void settleHoldSplit(Long milestoneId, BigDecimal freelancerAmount, BigDecimal clientRefundAmount) {
        Milestone milestone = milestoneRepository.findByIdWithProject(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        EscrowHold hold = escrowHoldRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));

        if (hold.getStatus() != EscrowHoldStatus.HELD) {
            throw new InvalidMilestoneStateException("Escrow hold is not in HELD status");
        }

        java.math.BigDecimal total = freelancerAmount.add(clientRefundAmount);
        if (total.compareTo(hold.getAmount()) != 0) {
            throw new IllegalArgumentException("freelancerAmount + clientRefundAmount must equal hold amount");
        }

        Project project = milestone.getProject();
        Instant now = Instant.now();

        if (freelancerAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (project.getFreelancer() == null) {
                throw new IllegalStateException("Project has no assigned freelancer");
            }
            Wallet freelancerWallet = walletService.findWalletByUserId(project.getFreelancer().getId());
            walletService.credit(freelancerWallet, freelancerAmount, ReferenceType.ESCROW_RELEASE, hold.getId());
        }
        if (clientRefundAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            walletService.credit(hold.getClientWallet(), clientRefundAmount, ReferenceType.ESCROW_REFUND, hold.getId());
        }

        if (freelancerAmount.compareTo(hold.getAmount()) == 0) {
            hold.setStatus(EscrowHoldStatus.RELEASED);
            milestone.setStatus(MilestoneStatus.APPROVED);
        } else if (clientRefundAmount.compareTo(hold.getAmount()) == 0) {
            hold.setStatus(EscrowHoldStatus.REFUNDED);
            milestone.setStatus(MilestoneStatus.REFUNDED);
        } else {
            hold.setStatus(EscrowHoldStatus.SPLIT);
            milestone.setStatus(MilestoneStatus.SETTLED);
        }
        hold.setResolvedAt(now);
        escrowHoldRepository.save(hold);
        milestone.setUpdatedAt(now);
        milestoneRepository.save(milestone);

        log.info(
                "Escrow split settled: milestoneId={} freelancerAmount={} clientRefund={}",
                milestoneId,
                freelancerAmount,
                clientRefundAmount);
    }
}
