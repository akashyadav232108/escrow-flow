package com.escrowflow.web.controller;

import com.escrowflow.security.SecurityUtils;
import com.escrowflow.service.WalletService;
import com.escrowflow.web.dto.AddFundsRequest;
import com.escrowflow.web.dto.AddFundsResponse;
import com.escrowflow.web.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse getWallet() {
        return walletService.getWallet(SecurityUtils.getCurrentUserId());
    }

    @PostMapping("/add-funds")
    public AddFundsResponse addFunds(@Valid @RequestBody AddFundsRequest request) {
        return walletService.addFunds(SecurityUtils.getCurrentUserId(), request.amount());
    }
}
