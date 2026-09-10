-- User moderation: account status + warnings audit trail.

ALTER TABLE users
    ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_users_account_status ON users (account_status);

CREATE TABLE user_warnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    issued_by_admin_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_warnings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_warnings_admin FOREIGN KEY (issued_by_admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_warnings_user ON user_warnings (user_id, created_at DESC);
