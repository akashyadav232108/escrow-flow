-- Admin support: track who created an admin, seed first SUPER_ADMIN.
-- Seed password (dev): SuperAdmin@123  — change immediately in production.

ALTER TABLE users
    ADD COLUMN created_by_user_id BIGINT NULL,
    ADD CONSTRAINT fk_users_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users (id);

CREATE INDEX idx_users_role ON users (role);

INSERT INTO users (name, email, password_hash, role, created_at, created_by_user_id)
SELECT 'Super Admin',
       'superadmin@escrowflow.local',
       '$2a$10$AGFUoq28ubl4SwlHY5OeAOpWYiaS9n6RmsQwTwfxpYCYaxM4ixkFC',
       'SUPER_ADMIN',
       CURRENT_TIMESTAMP,
       NULL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'superadmin@escrowflow.local'
);
