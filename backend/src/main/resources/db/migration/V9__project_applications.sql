-- Project applications (Phase A hiring): freelancers apply; client accepts one / declines others.
-- Until client accepts, project stays OPEN with no assigned freelancer.

CREATE TABLE project_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_project_applications_project_freelancer UNIQUE (project_id, freelancer_id),
    CONSTRAINT fk_project_applications_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_applications_freelancer FOREIGN KEY (freelancer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_project_applications_project_status ON project_applications (project_id, status);
CREATE INDEX idx_project_applications_freelancer_created ON project_applications (freelancer_id, created_at DESC);
