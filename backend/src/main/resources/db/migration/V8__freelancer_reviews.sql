-- Freelancer reviews: client rates assigned freelancer after successful work.
-- Eligibility (app-enforced): project client only; >= 1 milestone APPROVED; one review per project.

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    freelancer_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_reviews_project UNIQUE (project_id),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT fk_reviews_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_freelancer FOREIGN KEY (freelancer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reviews_freelancer_created ON reviews (freelancer_id, created_at DESC);
