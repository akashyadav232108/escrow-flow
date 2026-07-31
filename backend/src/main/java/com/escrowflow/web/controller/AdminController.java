package com.escrowflow.web.controller;

import com.escrowflow.service.AdminService;
import com.escrowflow.web.dto.AdminDashboardStatsResponse;
import com.escrowflow.web.dto.AdminUserResponse;
import com.escrowflow.web.dto.CreateAdminRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
