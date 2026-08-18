-- Project hire agreements: both parties acknowledge shared terms (no automatic penalties).
-- Created when a client accepts an application; work actions wait until both have accepted.

CREATE TABLE project_agreements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    terms_version VARCHAR(20) NOT NULL,
    terms_text TEXT NOT NULL,
    client_accepted_at TIMESTAMP NULL,
    freelancer_accepted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_project_agreements_project UNIQUE (project_id),
    CONSTRAINT fk_project_agreements_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
