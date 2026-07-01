CREATE TABLE escrow_holds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    milestone_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    client_wallet_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT uk_escrow_holds_milestone UNIQUE (milestone_id),
    CONSTRAINT fk_escrow_holds_milestone FOREIGN KEY (milestone_id) REFERENCES milestones (id),
    CONSTRAINT fk_escrow_holds_wallet FOREIGN KEY (client_wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_escrow_holds_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_escrow_holds_wallet ON escrow_holds (client_wallet_id);
