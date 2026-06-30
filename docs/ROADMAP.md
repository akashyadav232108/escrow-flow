# Build roadmap

8-week plan for the monolithic Escrow-Flow monorepo. Check items off as you complete them.

---

## Week 1–2: Auth + wallet foundation

**Goal**: Users can sign up, log in, and manage a wallet balance with full audit logging.

- [ ] Spring Boot project scaffold in `backend/`
- [ ] MySQL + Redis connection configuration
- [ ] Flyway `V1__init_schema.sql` — `users`, `wallets`, `wallet_transactions`
- [ ] JPA entities: `User`, `Wallet`, `WalletTransaction`
- [ ] `Wallet.version` with `@Version` for optimistic locking
- [ ] Signup endpoint — creates user + wallet (₹10,000 starting balance)
- [ ] Login endpoint — JWT issuance
- [ ] Spring Security — public auth routes, JWT filter for `/api/**`
- [ ] `GET /api/wallet` — current balance
- [ ] `POST /api/wallet/add-funds` — credit + audit row
- [ ] Integration test: add funds → balance matches transaction sum

**Exit criteria**: Postman/curl can signup, login, add funds, read balance.

---

## Week 3–4: Projects + milestones (no money)

**Goal**: Project lifecycle without escrow — CRUD and role-based access only.

- [ ] Flyway `V2__projects_milestones.sql`
- [ ] Entities: `Project`, `Milestone`
- [ ] Client creates project with milestone list (all `PENDING`)
- [ ] `GET /api/projects` — filtered by role (client vs freelancer)
- [ ] `GET /api/projects/{id}` — detail with milestones
- [ ] Freelancer accepts project → `freelancer_id` set, `IN_PROGRESS`
- [ ] Authorization guards (client-only create, freelancer-only accept)
- [ ] Integration tests for project create + accept

**Exit criteria**: Two users can create/accept a project; milestones visible; no balance changes.

---

## Week 5: Escrow core (money movement)

**Goal**: Full milestone escrow lifecycle with concurrency safety.

- [ ] Flyway `V3__escrow_holds.sql`
- [ ] Entity: `EscrowHold`
- [ ] `RedisWalletLockService` — SET NX PX + Lua release
- [ ] `EscrowService.lockFunds()` — atomic debit + hold + audit + `FUNDS_LOCKED`
- [ ] `MilestoneService.submit()` — `FUNDS_LOCKED` → `SUBMITTED`
- [ ] `EscrowService.approve()` — `RELEASED` + credit freelancer
- [ ] `EscrowService.dispute()` — `REFUNDED` + credit client
- [ ] `InvalidMilestoneStateException` for illegal transitions
- [ ] Integration test: lock → submit → approve (both wallet balances correct)
- [ ] Integration test: dispute refunds client
- [ ] Parallel lock test — only one debit succeeds

**Exit criteria**: Complete escrow cycle via API; balances and holds always consistent.

---

## Week 6: Idempotency + audit hardening

**Goal**: Production-grade safety patterns and observability.

- [ ] `IdempotencyService` — Redis cache for lock-funds responses (24h TTL)
- [ ] `Idempotency-Key` header required on `POST .../lock-funds`
- [ ] Duplicate idempotency key returns cached response, no second debit
- [ ] `GET /api/wallet/transactions` — paginated history
- [ ] `WalletConsistencyTest` — all wallets pass balance reconciliation
- [ ] Optional: dispute rate limit (`ratelimit:dispute:{userId}`)
- [ ] Error responses with stable error codes (see [API.md](API.md))

**Exit criteria**: Double-submit lock-funds is safe; audit log complete; consistency test passes.

---

## Week 7: React frontend

**Goal**: Four main views wired to the API.

- [ ] Vite + React + TypeScript + Redux Toolkit scaffold in `frontend/`
- [ ] Auth slice — login, signup, logout, token persistence
- [ ] Protected routes + layout navigation
- [ ] Dashboard — role-based project list
- [ ] Create project form with dynamic milestone rows
- [ ] Project detail — milestones with status badges
- [ ] Milestone action panel (lock / submit / approve / dispute)
- [ ] Wallet page — balance, add funds, transaction history
- [ ] Idempotency UUID generated on lock-funds click
- [ ] Refetch wallet after every money-affecting action
- [ ] Error toasts for 400/409 responses

**Exit criteria**: Full user journey doable in browser without curl.

---

## Week 8: EC2 deployment + polish

**Goal**: Public demo on AWS free tier.

- [ ] Extend `.gitignore` for Java, Node, secrets
- [ ] `application-prod.yml` / `.env` pattern documented
- [ ] EC2 instance provisioned (Ubuntu, security group)
- [ ] MySQL + Redis on instance (Docker Compose or native)
- [ ] Backend JAR via systemd
- [ ] Frontend `npm run build` → Nginx static root
- [ ] Nginx reverse proxy `/api` → Spring Boot
- [ ] Optional: HTTPS with Let's Encrypt
- [ ] End-to-end smoke test on live URL
- [ ] Root README quick-start verified

**Exit criteria**: Shareable URL; interviewer can signup and run through escrow flow.

---

## After v1 (optional enhancements)

- [ ] JWT logout blacklist in Redis
- [ ] File upload for milestone deliverables (S3)
- [ ] Admin dispute resolution panel
- [ ] Email notifications on submit/approve
- [ ] GitHub Actions CI (build + test on PR)
- [ ] OpenAPI / Swagger UI for API docs

---

## Dependency graph

```text
Week 1-2 (auth/wallet)
    └── Week 3-4 (projects)
            └── Week 5 (escrow) ← critical path
                    └── Week 6 (idempotency/audit)
                            └── Week 7 (frontend)
                                    └── Week 8 (deploy)
```

Do not start Week 5 until wallet debit/credit helpers and tests from Week 1-2 are solid.
