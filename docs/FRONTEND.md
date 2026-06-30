# Frontend implementation guide

React 18 SPA in `/frontend` with Redux Toolkit for state and Vite for tooling.

## Tech stack

- React 18 + TypeScript
- Redux Toolkit (`@reduxjs/toolkit`)
- React Router v6
- Axios (or fetch wrapper) for API calls
- Vite

## Project scaffold

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install @reduxjs/toolkit react-redux react-router-dom axios
```

```
frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── api/
│   │   ├── client.ts          # Axios instance + JWT interceptor
│   │   ├── authApi.ts
│   │   ├── walletApi.ts
│   │   └── projectApi.ts
│   ├── store/
│   │   ├── index.ts
│   │   └── slices/
│   │       ├── authSlice.ts
│   │       ├── walletSlice.ts
│   │       └── projectsSlice.ts
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── SignupPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── ProjectDetailPage.tsx
│   │   └── WalletPage.tsx
│   ├── components/
│   │   ├── Layout.tsx
│   │   ├── ProjectCard.tsx
│   │   ├── MilestoneList.tsx
│   │   ├── MilestoneActions.tsx
│   │   ├── WalletSummary.tsx
│   │   └── TransactionHistory.tsx
│   ├── types/
│   │   └── index.ts
│   └── utils/
│       └── idempotency.ts
├── .env.example
└── package.json
```

## Environment

`.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## Redux slices

### authSlice

| Field | Type |
|-------|------|
| `user` | `{ id, name, email, role } \| null` |
| `token` | `string \| null` |
| `isAuthenticated` | `boolean` |

**Thunks**: `signup`, `login`, `logout` (clear localStorage)

Persist token in `localStorage`; attach via Axios interceptor.

---

### walletSlice

| Field | Type |
|-------|------|
| `balance` | `number` |
| `transactions` | `Transaction[]` |
| `loading` | `boolean` |
| `error` | `string \| null` |

**Thunks**: `fetchWallet`, `fetchTransactions`, `addFunds`

**Critical**: after `lockFunds`, `approveMilestone`, or `disputeMilestone` — always `dispatch(fetchWallet())` so UI shows DB truth.

---

### projectsSlice

| Field | Type |
|-------|------|
| `projects` | `ProjectSummary[]` |
| `selectedProject` | `ProjectDetail \| null` |
| `loading` | `boolean` |

**Thunks**: `fetchProjects`, `fetchProjectById`, `createProject`, `acceptProject`, milestone actions

---

## Routes

| Path | Page | Guard |
|------|------|-------|
| `/login` | LoginPage | Public |
| `/signup` | SignupPage | Public |
| `/` | DashboardPage | Auth |
| `/projects/:id` | ProjectDetailPage | Auth |
| `/wallet` | WalletPage | Auth |

Wrap protected routes in `<RequireAuth>` that redirects to `/login`.

---

## Views

### 1. Dashboard

Role-aware project list:

- **Client**: "My projects" — projects I created
- **Freelancer**: "Open projects" + "My assignments"

Each card: title, status badge, milestone count, link to detail.

---

### 2. Project detail

- Project metadata (client, freelancer, status)
- `MilestoneList` with status badges:
  - `PENDING` → gray
  - `FUNDS_LOCKED` → blue
  - `SUBMITTED` → yellow
  - `APPROVED` → green
  - `DISPUTED` / `REFUNDED` → red
- `MilestoneActions` per row (see below)

**Client**: "Create project" form on dashboard (title, description, dynamic milestone rows).

**Freelancer**: "Accept project" button when `status === OPEN`.

---

### 3. Wallet

- Current balance (large display)
- "Add funds" modal (amount input)
- `TransactionHistory` table: date, type, amount, reference, balance after

---

### 4. Milestone action panel

Show actions based on **role + milestone.status** (mirror backend rules):

| Role | Status | Action |
|------|--------|--------|
| Client | PENDING | Lock funds |
| Freelancer | FUNDS_LOCKED | Submit work |
| Client | SUBMITTED | Approve / Dispute |
| Any | terminal | None (read-only) |

#### Lock funds — idempotency

```typescript
// utils/idempotency.ts
export function createIdempotencyKey(): string {
  return crypto.randomUUID();
}
```

On button click, generate key once and pass to API. If request fails with network error, **retry with same key**.

```typescript
const key = createIdempotencyKey();
await projectApi.lockFunds(milestoneId, key);
dispatch(fetchWallet());
```

---

## API client

```typescript
// api/client.ts
const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

---

## UX decisions

### Optimistic UI vs refetch

**Chosen**: refetch wallet after money actions.

| Approach | Pros | Cons |
|----------|------|------|
| Optimistic | Feels instant | Wrong balance if request fails |
| Refetch (chosen) | Always correct | Slight delay |

For fintech demo, correctness wins.

### Error display

Show API `message` in toast or inline alert. On `409 WALLET_BUSY`, enable retry button.

### Loading states

Disable action buttons while request in flight — prevents double-click (idempotency is backup, not primary UX).

---

## Phase checklist (Week 7)

- [ ] Vite + RTK scaffold
- [ ] Auth pages + token persistence
- [ ] Protected layout with nav (Dashboard, Wallet, Logout)
- [ ] Dashboard with project list
- [ ] Create project form with dynamic milestones
- [ ] Project detail + milestone badges
- [ ] MilestoneActions (lock, submit, approve, dispute)
- [ ] Wallet page + transaction history
- [ ] Idempotency key on lock funds
- [ ] Refetch wallet after escrow actions
- [ ] Basic responsive layout

---

## Build for production

```bash
npm run build
```

Output in `frontend/dist/` — served by Nginx on EC2 (see [DEPLOYMENT.md](DEPLOYMENT.md)).
