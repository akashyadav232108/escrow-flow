-- Project exit disputes (Phase B): mid-project leave/swap request; admin splits held escrow per milestone.

CREATE TABLE project_exits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    raised_by_user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    project_outcome VARCHAR(20) NULL,
    admin_note TEXT NULL,
    resolved_by_admin_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT fk_project_exits_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_exits_raised_by FOREIGN KEY (raised_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_project_exits_resolved_by FOREIGN KEY (resolved_by_admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_project_exits_status ON project_exits (status, created_at DESC);
CREATE INDEX idx_project_exits_project ON project_exits (project_id, status);

CREATE TABLE project_exit_settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_exit_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    hold_amount DECIMAL(19, 4) NOT NULL,
    freelancer_amount DECIMAL(19, 4) NULL,
    client_refund_amount DECIMAL(19, 4) NULL,
    CONSTRAINT uk_project_exit_settlements_exit_milestone UNIQUE (project_exit_id, milestone_id),
    CONSTRAINT fk_project_exit_settlements_exit FOREIGN KEY (project_exit_id) REFERENCES project_exits (id),
    CONSTRAINT fk_project_exit_settlements_milestone FOREIGN KEY (milestone_id) REFERENCES milestones (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
