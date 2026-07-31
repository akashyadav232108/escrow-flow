package com.escrowflow.web.dto;

import java.math.BigDecimal;

public record AdminDashboardStatsResponse(
        long totalUsers,
        long clients,
        long freelancers,
        long both,
        long admins,
        long warnedUsers,
        long suspendedUsers,
        long openProjects,
        long inProgressProjects,
        long completedProjects,
        long cancelledProjects,
        BigDecimal totalEscrowHeld,
        long disputedMilestones
) {
}
