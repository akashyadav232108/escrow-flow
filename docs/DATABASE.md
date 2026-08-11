# Database schema

MySQL 8, InnoDB engine. All monetary fields use `DECIMAL(19,4)` to avoid floating-point errors.

## ER overview

```
users ─────┬──── wallets
           │
           ├──── projects (client_id, freelancer_id)
           │         │
           │         ├── milestones
           │         │         │
           │         │         ├── escrow_holds (1:1 per milestone)
           │         │         └── disputes (1:1 per milestone, when raised)
           │         │
           │         └── project_applications
           │
           ├──── reviews
           │
           ├──── notifications
           │
           └── wallet_transactions (via wallet_id)
```

## Tables

### users

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| role | VARCHAR(20) | NOT NULL — `CLIENT`, `FREELANCER`, `BOTH`, `ADMIN`, or `SUPER_ADMIN` |
| account_status | VARCHAR(20) | NOT NULL — `ACTIVE`, `WARNED`, `SUSPENDED`, `DELETED` (default `ACTIVE`) |
| created_by_user_id | BIGINT | NULL, FK → users(id) — set when an admin creates another admin |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| deleted_at | TIMESTAMP | NULL — set on soft delete |

**Notes**

- Marketplace roles: `CLIENT`, `FREELANCER`, `BOTH`. Admin roles are exclusive — an admin cannot also be a client/freelancer.
- `ADMIN` / `SUPER_ADMIN` cannot self-register via public signup. First `SUPER_ADMIN` is seeded by Flyway (`V4__admin_support.sql`).
- Dev seed login: `superadmin@escrowflow.local` / `SuperAdmin@123` — change in production.
- Passwords stored with BCrypt only — never plain text.
- `created_by_user_id` tracks which admin provisioned another admin (null for self-signup and the seed super admin).
- Suspended/deleted users cannot log in; existing JWTs are rejected by the auth filter.

---

### wallets

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL, UNIQUE, FK → users(id) |
| balance | DECIMAL(19,4) | NOT NULL, DEFAULT 0 |
| version | INT | NOT NULL, DEFAULT 0 — optimistic locking |
| updated_at | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP |

**Notes**

- One wallet per user — enforce with `UNIQUE(user_id)`.
- Application must reject debits that would make balance negative.
- `version` used with JPA `@Version`; failed updates throw `OptimisticLockException`.

---

### projects

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| client_id | BIGINT | NOT NULL, FK → users(id) |
| freelancer_id | BIGINT | NULL, FK → users(id) |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | |
| status | VARCHAR(20) | NOT NULL — `OPEN`, `IN_PROGRESS`, `EXIT_DISPUTED`, `COMPLETED`, `CANCELLED` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**Status transitions**

- `OPEN` — created, no freelancer yet (freelancers may apply)
- `IN_PROGRESS` — client accepted an application (or legacy instant accept)
- `EXIT_DISPUTED` — project exit open; milestone money actions frozen until admin resolves
- `COMPLETED` — all milestones approved (optional auto-transition)
- `CANCELLED` — abandoned / exit cancelled

---

### project_applications

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| project_id | BIGINT | NOT NULL, FK → projects(id) |
| freelancer_id | BIGINT | NOT NULL, FK → users(id) |
| status | VARCHAR(20) | NOT NULL — `PENDING`, `ACCEPTED`, `DECLINED`, `WITHDRAWN` |
| message | TEXT | NULL — optional cover note |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

**Rules**

- `UNIQUE(project_id, freelancer_id)` — one application per freelancer per project.
- Apply only while project is `OPEN` and `freelancer_id` is null.
- Client accept: set application `ACCEPTED`, assign `projects.freelancer_id`, project → `IN_PROGRESS`, decline other `PENDING` rows.
- Indexes: `(project_id, status)`, `(freelancer_id, created_at DESC)`.

---

### milestones

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| project_id | BIGINT | NOT NULL, FK → projects(id) |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | |
| amount | DECIMAL(19,4) | NOT NULL, CHECK amount > 0 |
| status | VARCHAR(20) | NOT NULL — see state machine below |
| submitted_note | TEXT | NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP |

**Status values**

| Status | Meaning |
|--------|---------|
| `PENDING` | Created, funds not locked |
| `FUNDS_LOCKED` | Client debited, escrow hold active |
| `SUBMITTED` | Freelancer submitted work |
| `APPROVED` | Client approved; funds released to freelancer |
| `DISPUTED` | Dispute raised; escrow stays `HELD` until admin resolves |
| `REFUNDED` | Funds returned to client after dispute/exit; client may **re-lock** (reuse same hold → `HELD`) |
| `SETTLED` | Project-exit partial split (terminal) |

---

### project_exits

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK |
| project_id | BIGINT | NOT NULL, FK → projects |
| raised_by_user_id | BIGINT | NOT NULL, FK → users |
| reason | TEXT | NOT NULL |
| status | VARCHAR(20) | `OPEN`, `RESOLVED` |
| project_outcome | VARCHAR(20) | NULL — `CANCELLED`, `REOPEN` on resolve |
| admin_note | TEXT | NULL |
| resolved_by_admin_id | BIGINT | NULL |
| created_at / resolved_at | TIMESTAMP | |

### project_exit_settlements

One row per held milestone at raise time: `hold_amount` snapshot; on resolve `freelancer_amount` + `client_refund_amount` (sum = hold).

---

### disputes

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| milestone_id | BIGINT | NOT NULL, UNIQUE, FK → milestones(id) |
| raised_by_user_id | BIGINT | NOT NULL, FK → users(id) |
| reason | TEXT | NOT NULL |
| status | VARCHAR(20) | NOT NULL — `OPEN`, `RESOLVED` |
| resolution | VARCHAR(30) | NULL — `FREELANCER_WINS`, `CLIENT_WINS` |
| resolved_by_admin_id | BIGINT | NULL, FK → users(id) |
| admin_note | TEXT | NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| resolved_at | TIMESTAMP | NULL |

**Notes**

- Raising a dispute does **not** move money — escrow hold stays `HELD`.
- Admin resolve: `FREELANCER_WINS` → release to freelancer; `CLIENT_WINS` → refund client.
- Client or assigned freelancer may raise a dispute while milestone is `SUBMITTED`.

---

### user_warnings

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL, FK → users(id) |
| issued_by_admin_id | BIGINT | NOT NULL, FK → users(id) |
| reason | TEXT | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**Notes**

- Append-only audit of warnings / suspend / delete reasons.
- Soft delete is blocked while the user has open disputes, held escrow, or in-progress projects.

---

### notifications

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL, FK → users(id) |
| type | VARCHAR(40) NOT NULL — see enum below |
| title | VARCHAR(255) | NOT NULL |
| message | TEXT | NOT NULL |
| reference_type | VARCHAR(30) | NULL — `PROJECT`, `MILESTONE`, `DISPUTE` |
| reference_id | BIGINT | NULL — id of the referenced entity |
| is_read | BOOLEAN | NOT NULL, DEFAULT FALSE |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**type values**

| Value | When |
|-------|------|
| `PROJECT_CREATED` | Client creates an open project (fan-out to freelancers) |
| `WORK_SUBMITTED` | Freelancer submits milestone work (notify client) |
| `DISPUTE_RAISED` | Dispute raised (notify other party + admins) |
| `DISPUTE_RESOLVED` | Admin resolves dispute (notify client + freelancer) |
| `REVIEW_RECEIVED` | Client leaves a review (notify freelancer; `referenceType` = `PROJECT`) |
| `APPLICATION_RECEIVED` | Freelancer applies (notify client; `referenceType` = `PROJECT`) |
| `APPLICATION_ACCEPTED` | Client accepts application (notify freelancer) |
| `APPLICATION_DECLINED` | Client declines application (notify freelancer) |
| `PROJECT_EXIT_RAISED` | Project exit requested (other party + admins) |
| `PROJECT_EXIT_RESOLVED` | Admin resolved project exit (client + freelancer) |

**Notes**

- Rows are inserted inside the same `@Transactional` business flow (no message broker).
- Recipients only see their own notifications via `/api/notifications`.
- Index: `(user_id, is_read, created_at DESC)`.

---

### escrow_holds

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| milestone_id | BIGINT | NOT NULL, UNIQUE, FK → milestones(id) |
| amount | DECIMAL(19,4) | NOT NULL |
| client_wallet_id | BIGINT | NOT NULL, FK → wallets(id) |
| status | VARCHAR(20) | NOT NULL — `HELD`, `RELEASED`, `REFUNDED` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| resolved_at | TIMESTAMP | NULL |

**Notes**

- `UNIQUE(milestone_id)` — at most one hold per milestone.
- `amount` should match `milestones.amount` at lock time.
- Terminal states: `RELEASED` (paid to freelancer). `REFUNDED` may be re-locked (same hold row → `HELD` again); wallet tx history is append-only.
- `SPLIT` — project-exit partial payout (terminal for that settlement round).

---

### wallet_transactions (append-only)

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| wallet_id | BIGINT | NOT NULL, FK → wallets(id) |
| type | VARCHAR(10) | NOT NULL — `CREDIT`, `DEBIT` |
| amount | DECIMAL(19,4) | NOT NULL, CHECK amount > 0 |
| reference_type | VARCHAR(30) | NOT NULL — see enum below |
| reference_id | BIGINT | NULL — escrow_hold id when applicable |
| balance_after | DECIMAL(19,4) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**reference_type values**

| Value | When |
|-------|------|
| `ADD_FUNDS` | User tops up wallet |
| `ESCROW_LOCK` | Client locks milestone funds |
| `ESCROW_RELEASE` | Freelancer paid on approval |
| `ESCROW_REFUND` | Client refunded on dispute |

**Rules**

- Never `UPDATE` or `DELETE` rows in this table.
- Every change to `wallets.balance` must insert exactly one row here.
- `balance_after` is a snapshot at write time — invaluable for debugging and audits.

---

## Indexes (recommended)

```sql
CREATE INDEX idx_projects_client ON projects(client_id);
CREATE INDEX idx_projects_freelancer ON projects(freelancer_id);
CREATE INDEX idx_milestones_project ON milestones(project_id);
CREATE INDEX idx_wallet_txn_wallet ON wallet_transactions(wallet_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);
```

---

## Invariants to test

1. **Balance reconciliation**

   ```text
   wallets.balance == SUM(CREDIT amounts) - SUM(DEBIT amounts)
   ```

   for all transactions on that wallet.

2. **Non-negative balance** — `wallets.balance >= 0` always.

3. **Escrow amount consistency** — `escrow_holds.amount == milestones.amount` at lock time.

4. **Hold uniqueness** — no second `HELD` row for the same milestone.

5. **State alignment** — e.g. milestone `FUNDS_LOCKED` implies an `escrow_holds` row with status `HELD`.

---

## Transaction isolation

Use Spring default `READ_COMMITTED` for MySQL. Combined with Redis wallet lock and `@Version`, this is sufficient for this workload.

Consider `REPEATABLE_READ` only if you observe phantom reads in complex reports — not required for v1.

---

## Sample DDL (reference)

Full migration will live in `backend/src/main/resources/db/migration/` (Flyway) or `schema.sql`. Example:

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- See Flyway migrations in backend for full schema
```
