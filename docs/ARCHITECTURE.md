# Architecture

Escrow-Flow is a **monolithic** Spring Boot backend with a React SPA frontend. The backend owns all money movement, state transitions, and audit logging. The frontend is a thin client that never computes balances or enforces business rules.

## High-level diagram

```
┌─────────────┐     HTTPS      ┌──────────────┐
│  React SPA  │ ──────────────▶│    Nginx     │
│  (Redux)    │                │  (EC2 prod)  │
└─────────────┘                └──────┬───────┘
                                      │ /api → :8080
                                      │ /    → static
                               ┌──────▼───────┐
                               │ Spring Boot  │
                               │  (monolith)  │
                               └──┬────────┬──┘
                                  │        │
                            ┌─────▼──┐  ┌──▼────┐
                            │ MySQL  │  │ Redis │
                            │ (SoT)  │  │(safety)│
                            └────────┘  └───────┘
```

## Domain model (6 tables)

| Entity | Role |
|--------|------|
| `User` | Client and/or freelancer identity |
| `Wallet` | One per user; holds spendable balance |
| `Project` | Client-owned work agreement; optional freelancer |
| `Milestone` | Unit of work + escrow amount |
| `EscrowHold` | Locked funds for one milestone |
| `WalletTransaction` | Append-only audit log for every balance change |

See [DATABASE.md](DATABASE.md) for column-level detail.

## Core flows

### 1. Signup

1. Create `User` with hashed password.
2. Create `Wallet` with starting balance (e.g. ₹10,000).
3. Return JWT.

### 2. Lock funds (client)

Triggered when client funds a `PENDING` milestone.

**Inside one `@Transactional` block** (after acquiring Redis wallet lock):

1. Validate milestone is `PENDING`, project belongs to client, balance ≥ amount.
2. Debit `Wallet.balance` (client).
3. Insert `EscrowHold` with status `HELD`.
4. Insert `WalletTransaction` (type `DEBIT`, reference `ESCROW_LOCK`).
5. Update milestone status → `FUNDS_LOCKED`.

### 3. Submit work (freelancer)

1. Validate milestone is `FUNDS_LOCKED`, freelancer owns project.
2. Set `submitted_note`, status → `SUBMITTED`.

No money movement.

### 4. Approve (client)

**Inside one `@Transactional` block**:

1. Validate milestone is `SUBMITTED`.
2. Update `EscrowHold` → `RELEASED`, set `resolved_at`.
3. Credit freelancer `Wallet.balance`.
4. Insert `WalletTransaction` on freelancer wallet (`CREDIT`, `ESCROW_RELEASE`).
5. Milestone status → `APPROVED`.

### 5. Dispute (client)

Same structure as approve, but:

- `EscrowHold` → `REFUNDED`
- Credit **client** wallet (`ESCROW_REFUND`)
- Milestone status → `DISPUTED` or `REFUNDED` (pick one and stay consistent)

## Milestone state machine

```
                    lock funds
    PENDING ──────────────────▶ FUNDS_LOCKED
                                    │
                              submit work
                                    │
                                    ▼
                               SUBMITTED ────── approve ──────▶ APPROVED
                                    │
                                    └────── dispute ──────▶ DISPUTED / REFUNDED
```

Illegal transitions must throw `InvalidMilestoneStateException` in the service layer — never rely on the frontend.

## Concurrency strategy

Two complementary layers:

| Layer | Mechanism | Purpose |
|-------|-----------|---------|
| Redis | `lock:wallet:{walletId}` SET NX PX | Serialize wallet debits/credits |
| MySQL | `wallets.version` + `@Version` | Detect lost updates if lock fails |

**Recommended**: use Redis lock as primary guard; `@Version` as belt-and-suspenders.

### Why not check-then-act?

```text
Thread A: read balance 1000
Thread B: read balance 1000
Thread A: debit 800 → write 200
Thread B: debit 600 → write 400   ← should have failed; both saw 1000
```

Balance check and debit must happen in the **same transaction**, under a **single-writer lock**.

## Idempotency

`POST /milestones/{id}/lock-funds` accepts `Idempotency-Key` header (client-generated UUID).

- First request: process normally, cache response in Redis for 24h.
- Retry / double-click: return cached response without re-debiting.

See [REDIS.md](REDIS.md).

## Audit invariant

Every balance change produces **exactly one** `wallet_transactions` row with `balance_after` snapshot.

```text
wallet.balance == SUM(credits) - SUM(debits)   // from wallet_transactions
```

Run this as an integration test and optionally as a scheduled consistency check.

## Sequence: lock funds with idempotency

```mermaid
sequenceDiagram
    participant C as Client UI
    participant API as Spring Boot
    participant R as Redis
    participant DB as MySQL

    C->>API: POST lock-funds (Idempotency-Key)
    API->>R: GET idem:key
    alt key exists
        R-->>API: cached response
        API-->>C: 200 (cached)
    else new request
        API->>R: SET lock:wallet NX PX 5000
        alt lock not acquired
            API-->>C: 409 Try again
        else lock acquired
            API->>DB: BEGIN; debit; hold; txn; COMMIT
            API->>R: DEL lock (Lua, if value matches)
            API->>R: SETEX idem:key response 24h
            API-->>C: 200
        end
    end
```

## Backend package layout

```
com.escrowflow
├── config/           Security, Redis, JPA
├── domain/           JPA entities
├── repository/       Spring Data JPA
├── service/          Business logic + @Transactional
├── infrastructure/   Redis lock, idempotency store
├── web/              Controllers, DTOs, exception handlers
└── EscrowFlowApplication.java
```

## Frontend responsibilities

- Auth token storage and API calls
- Role-based UI (show lock vs submit vs approve)
- Generate idempotency UUID on "Lock Funds" click
- **Refetch wallet** after any money-affecting action (source of truth over optimistic UI)

See [FRONTEND.md](FRONTEND.md).

## What this design deliberately avoids

- Redis as a general-purpose cache for projects/users
- Microservices for a demo-scale escrow flow
- Trusting frontend for balance or state validation
