-- Disputes: freeze escrow on raise; admin resolves to release or refund.

CREATE TABLE disputes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    milestone_id BIGINT NOT NULL,
    raised_by_user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolution VARCHAR(30) NULL,
    resolved_by_admin_id BIGINT NULL,
    admin_note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT uk_disputes_milestone UNIQUE (milestone_id),
    CONSTRAINT fk_disputes_milestone FOREIGN KEY (milestone_id) REFERENCES milestones (id),
    CONSTRAINT fk_disputes_raised_by FOREIGN KEY (raised_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_disputes_resolved_by FOREIGN KEY (resolved_by_admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_disputes_status ON disputes (status);
