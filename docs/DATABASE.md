# Database schema

MySQL 8, InnoDB engine. All monetary fields use `DECIMAL(19,4)` to avoid floating-point errors.

## ER overview

```
users ─────┬──── wallets
           │
           ├──── projects (client_id, freelancer_id)
           │         │
           │         └── milestones
           │                   │
           │                   └── escrow_holds (1:1 per milestone)
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
| role | VARCHAR(20) | NOT NULL — `CLIENT`, `FREELANCER`, or `BOTH` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**Notes**

- A user can be client, freelancer, or both. For v1, a single `role` column is enough; a join table can come later if roles need to be independent.
- Passwords stored with BCrypt only — never plain text.

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
| status | VARCHAR(20) | NOT NULL — `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**Status transitions**

- `OPEN` — created, no freelancer yet
- `IN_PROGRESS` — freelancer accepted
- `COMPLETED` — all milestones approved (optional auto-transition)
- `CANCELLED` — abandoned before work starts

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
| `DISPUTED` | Client disputed submitted work |
| `REFUNDED` | Funds returned to client (terminal) |

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
- Terminal states: `RELEASED` (paid to freelancer), `REFUNDED` (returned to client).

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
