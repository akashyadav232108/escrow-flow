package com.escrowflow.service;

import com.escrowflow.domain.EscrowHold;
import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Project;
import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.ReferenceType;
import com.escrowflow.infrastructure.RedisWalletLockService;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.MilestoneRepository;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.InvalidMilestoneStateException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
public class EscrowService {

    private final MilestoneRepository milestoneRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final WalletService walletService;
    private final RedisWalletLockService lockService;

    public EscrowService(
            MilestoneRepository milestoneRepository,
            EscrowHoldRepository escrowHoldRepository,
            WalletService walletService,
            RedisWalletLockService lockService) {
        this.milestoneRepository = milestoneRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.walletService = walletService;
        this.lockService = lockService;
    }

    public void lockFunds(Long milestoneId, Long clientUserId) {
        Milestone milestone = milestoneRepository.findByIdWithProject(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        Project project = milestone.getProject();
        
        if (!project.getClient().getId().equals(clientUserId)) {
            throw new ForbiddenException("Only the project client can lock funds");
        }

        if (milestone.getStatus() != MilestoneStatus.PENDING) {
            throw new InvalidMilestoneStateException(
                    "Cannot lock funds for milestone in status: " + milestone.getStatus());
        }

        Wallet clientWallet = walletService.findWalletByUserId(clientUserId);
        String lockRequestId = lockService.acquireLock(clientWallet.getId());

        try {
            lockFundsTransactional(milestone, clientWallet);
            log.info("Funds locked: milestoneId={} amount={} clientWalletId={}", 
                    milestoneId, milestone.getAmount(), clientWallet.getId());
        } finally {
            lockService.releaseLock(clientWallet.getId(), lockRequestId);
        }
    }

    @Transactional
    protected void lockFundsTransactional(Milestone milestone, Wallet clientWallet) {
        walletService.debit(clientWallet, milestone.getAmount(), ReferenceType.ESCROW_LOCK, null);

        EscrowHold hold = EscrowHold.builder()
                .milestone(milestone)
                .amount(milestone.getAmount())
                .clientWallet(clientWallet)
                .status(EscrowHoldStatus.HELD)
                .build();
        escrowHoldRepository.save(hold);

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

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new InvalidMilestoneStateException(
                    "Cannot approve milestone in status: " + milestone.getStatus());
        }

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

        log.info("Milestone approved: milestoneId={} amount={} freelancerWalletId={}", 
                milestoneId, hold.getAmount(), freelancerWallet.getId());
    }

    @Transactional
    public void dispute(Long milestoneId, Long clientUserId, String reason) {
        Milestone milestone = milestoneRepository.findByIdWithProject(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        Project project = milestone.getProject();

        if (!project.getClient().getId().equals(clientUserId)) {
            throw new ForbiddenException("Only the project client can dispute milestone");
        }

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new InvalidMilestoneStateException(
                    "Cannot dispute milestone in status: " + milestone.getStatus());
        }

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

        log.info("Milestone disputed and refunded: milestoneId={} amount={} reason={}", 
                milestoneId, hold.getAmount(), reason);
    }
}
