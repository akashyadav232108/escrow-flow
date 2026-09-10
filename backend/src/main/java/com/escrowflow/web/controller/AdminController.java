package com.escrowflow.web.controller;

import com.escrowflow.domain.enums.AccountStatus;
import com.escrowflow.domain.enums.DisputeStatus;
import com.escrowflow.service.AdminService;
import com.escrowflow.web.dto.AdminDashboardStatsResponse;
import com.escrowflow.web.dto.AdminUserResponse;
import com.escrowflow.web.dto.CreateAdminRequest;
import com.escrowflow.web.dto.DisputeResponse;
import com.escrowflow.web.dto.ManagedUserResponse;
import com.escrowflow.web.dto.ModerationReasonRequest;
import com.escrowflow.web.dto.ResolveDisputeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return adminService.createAdmin(request);
    }

    @GetMapping("/admins")
    public List<AdminUserResponse> listAdmins() {
        return adminService.listAdmins();
    }

    @GetMapping("/dashboard")
    public AdminDashboardStatsResponse getDashboardStats() {
        return adminService.getDashboardStats();
    }

    @GetMapping("/disputes")
    public List<DisputeResponse> listDisputes(
            @RequestParam(required = false) DisputeStatus status) {
        return adminService.listDisputes(status);
    }

    @GetMapping("/disputes/{id}")
    public DisputeResponse getDispute(@PathVariable Long id) {
        return adminService.getDispute(id);
    }

    @PostMapping("/disputes/{id}/resolve")
    public DisputeResponse resolveDispute(
            @PathVariable Long id,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return adminService.resolveDispute(id, request);
    }

    @GetMapping("/users")
    public List<ManagedUserResponse> listUsers(
            @RequestParam(required = false) AccountStatus status) {
        return adminService.listUsers(status);
    }

    @GetMapping("/users/{id}")
    public ManagedUserResponse getUser(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    @PostMapping("/users/{id}/warnings")
    public ManagedUserResponse warnUser(
            @PathVariable Long id,
            @Valid @RequestBody ModerationReasonRequest request) {
        return adminService.warnUser(id, request);
    }

    @PostMapping("/users/{id}/suspend")
    public ManagedUserResponse suspendUser(
            @PathVariable Long id,
            @Valid @RequestBody ModerationReasonRequest request) {
        return adminService.suspendUser(id, request);
    }

    @PostMapping("/users/{id}/unsuspend")
    public ManagedUserResponse unsuspendUser(@PathVariable Long id) {
        return adminService.unsuspendUser(id);
    }

    @PostMapping("/users/{id}/delete")
    public ManagedUserResponse softDeleteUser(
            @PathVariable Long id,
            @Valid @RequestBody ModerationReasonRequest request) {
        return adminService.softDeleteUser(id, request);
    }
}
