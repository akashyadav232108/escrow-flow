package com.escrowflow.web.controller;

import com.escrowflow.domain.EscrowHold;
import com.escrowflow.domain.Milestone;
import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.infrastructure.IdempotencyService;
import com.escrowflow.repository.EscrowHoldRepository;
import com.escrowflow.repository.MilestoneRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.service.EscrowService;
import com.escrowflow.service.MilestoneService;
import com.escrowflow.service.WalletService;
import com.escrowflow.web.dto.DisputeRequest;
import com.escrowflow.web.dto.LockFundsResponse;
import com.escrowflow.web.dto.MilestoneActionResponse;
import com.escrowflow.web.dto.SubmitWorkRequest;
import com.escrowflow.web.exception.IdempotencyKeyConflictException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final EscrowService escrowService;
    private final MilestoneService milestoneService;
    private final MilestoneRepository milestoneRepository;
    private final EscrowHoldRepository escrowHoldRepository;
    private final WalletService walletService;
    private final IdempotencyService idempotencyService;

    public MilestoneController(
            EscrowService escrowService,
            MilestoneService milestoneService,
            MilestoneRepository milestoneRepository,
            EscrowHoldRepository escrowHoldRepository,
            WalletService walletService,
            IdempotencyService idempotencyService) {
        this.escrowService = escrowService;
        this.milestoneService = milestoneService;
        this.milestoneRepository = milestoneRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.walletService = walletService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/{id}/lock-funds")
    public LockFundsResponse lockFunds(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        Optional<LockFundsResponse> cached =
                idempotencyService.getCachedResponse(idempotencyKey, LockFundsResponse.class);

        if (cached.isPresent()) {
            LockFundsResponse cachedResponse = cached.get();
            if (!cachedResponse.milestoneId().equals(id)) {
                throw new IdempotencyKeyConflictException(
                        "Idempotency-Key '" + idempotencyKey + "' was already used for milestone "
                                + cachedResponse.milestoneId() + "; cannot reuse it for milestone " + id);
            }
            return cachedResponse;
        }

        Long userId = SecurityUtils.getCurrentUserId();
        escrowService.lockFunds(id, userId);

        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        EscrowHold hold = escrowHoldRepository.findByMilestoneId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));
        Wallet wallet = walletService.findWalletByUserId(userId);

        LockFundsResponse response = new LockFundsResponse(
                milestone.getId(),
                milestone.getStatus(),
                hold.getId(),
                wallet.getBalance()
        );

        idempotencyService.cacheResponse(idempotencyKey, response);
        return response;
    }

    @PostMapping("/{id}/submit")
    public MilestoneActionResponse submit(
            @PathVariable Long id,
            @Valid @RequestBody SubmitWorkRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        milestoneService.submit(id, userId, request.note());

        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        return new MilestoneActionResponse(
                milestone.getId(),
                milestone.getStatus(),
                null
        );
    }

    @PostMapping("/{id}/approve")
    public MilestoneActionResponse approve(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        escrowService.approve(id, userId);

        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        EscrowHold hold = escrowHoldRepository.findByMilestoneId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));

        return new MilestoneActionResponse(
                milestone.getId(),
                milestone.getStatus(),
                hold.getStatus()
        );
    }

    @PostMapping("/{id}/dispute")
    public MilestoneActionResponse dispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        escrowService.dispute(id, userId, request.reason());

        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        EscrowHold hold = escrowHoldRepository.findByMilestoneId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow hold not found"));

        return new MilestoneActionResponse(
                milestone.getId(),
                milestone.getStatus(),
                hold.getStatus()
        );
    }
}
