package com.escrowflow.security;

import com.escrowflow.domain.enums.UserRole;
import com.escrowflow.web.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static UserRole getCurrentRole() {
        return getCurrentUser().getRole();
    }

    public static void requireAdmin() {
        UserRole role = getCurrentRole();
        if (!role.isAdminRole()) {
            throw new ForbiddenException("Admin access required");
        }
    }

    public static void requireSuperAdmin() {
        if (getCurrentRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Super admin access required");
        }
    }
}
