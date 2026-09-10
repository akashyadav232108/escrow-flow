package com.escrowflow.domain.enums;

public enum UserRole {
    CLIENT,
    FREELANCER,
    BOTH,
    ADMIN,
    SUPER_ADMIN;

    public boolean isAdminRole() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean isMarketplaceRole() {
        return this == CLIENT || this == FREELANCER || this == BOTH;
    }
}
