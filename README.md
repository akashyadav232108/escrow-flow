# Escrow-Flow

A milestone-based escrow system for freelance payments. Clients lock funds per milestone; freelancers submit work; clients approve or dispute. Built for **correctness under concurrency** — not generic caching.

## Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3, Java 17+, MySQL 8 |
| Locks / safety | Redis 7 |
| Frontend | React 18, Redux Toolkit, Vite |
| Auth | JWT (stateless) |
| Deploy | AWS EC2 (free tier), Nginx reverse proxy |

## Repository structure

```
/backend   — Spring Boot API (monolith)
/frontend  — React + Redux SPA
/docs      — Design docs and task breakdown
```

## Core concepts

- **6 tables**: `users`, `wallets`, `projects`, `milestones`, `escrow_holds`, `wallet_transactions`
- **Redis (correctness, not cache)**: wallet distributed locks, idempotency keys, optional dispute rate limits
- **Concurrency**: Redis lock + JPA `@Version` on wallets
- **Audit**: append-only `wallet_transactions`; balance must equal sum of transactions

## Documentation

| Doc | Purpose |
|-----|---------|
| [Architecture](docs/ARCHITECTURE.md) | Flows, state machines, design decisions |
| [Database](docs/DATABASE.md) | Schema, FKs, constraints, invariants |
| [Redis](docs/REDIS.md) | Lock, idempotency, rate limit patterns |
| [API](docs/API.md) | REST endpoints and contracts |
| [Backend tasks](docs/BACKEND.md) | Spring Boot phased implementation |
| [Frontend tasks](docs/FRONTEND.md) | React views and Redux slices |
| [Deployment](docs/DEPLOYMENT.md) | EC2 setup guide |
| [Roadmap](docs/ROADMAP.md) | 8-week build order |

## Local development (quick start)

### Prerequisites

- Java 17+, Maven 3.9+
- Node.js 20+, npm
- MySQL 8, Redis 7 (or Docker Compose — see [Deployment](docs/DEPLOYMENT.md))

### Backend

```bash
cd backend
cp src/main/resources/application-example.yml application-local.yml
# Configure DB URL, Redis host, JWT secret
./mvnw spring-boot:run
```

API: `http://localhost:8080`

### Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

App: `http://localhost:5173`

## Interview talking points

1. Atomic multi-table writes (`@Transactional`) for lock / release / refund
2. Lost-update prevention: Redis lock + optimistic locking (`@Version`)
3. Idempotency keys on fund-lock (double-click / network retry safe)
4. Milestone state machine enforced in the service layer
5. Audit invariant: `wallet.balance == SUM(transactions)`

## License

MIT — see [LICENSE](LICENSE).
