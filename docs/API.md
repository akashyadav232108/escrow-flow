# API reference

Base URL: `http://localhost:8080/api` (dev) or `https://your-domain/api` (prod).

All authenticated endpoints require:

```http
Authorization: Bearer <jwt>
```

Money-affecting lock endpoint also requires:

```http
Idempotency-Key: <client-generated-uuid>
```

---

## Auth

### POST `/auth/signup`

Create user and wallet with starting balance.

**Request**

```json
{
  "name": "Jane Client",
  "email": "jane@example.com",
  "password": "securePassword123",
  "role": "CLIENT"
}
```

`role`: `CLIENT` | `FREELANCER` | `BOTH`

Admin roles (`ADMIN`, `SUPER_ADMIN`) **cannot** be self-registered here — they are provisioned separately.

**Errors**

| Status | When |
|--------|------|
| 400 | `role` is `ADMIN` or `SUPER_ADMIN` |
| 409 | Email already registered |

**Response** `201`

```json
{
  "token": "eyJ...",
  "user": {
    "id": 1,
    "name": "Jane Client",
    "email": "jane@example.com",
    "role": "CLIENT"
  }
}
```

---

### POST `/auth/login`

**Request**

```json
{
  "email": "jane@example.com",
  "password": "securePassword123"
}
```

**Response** `200` — same shape as signup.

---

### POST `/auth/change-password`

Authenticated user changes their own password.

**Request**

```json
{
  "currentPassword": "securePassword123",
  "newPassword": "evenMoreSecure456"
}
```

**Response** `204` — no body.

**Errors**

| Status | When |
|--------|------|
| 401 | `currentPassword` does not match |
| 400 | `newPassword` shorter than 8 characters |

---

## Wallet

### GET `/wallet`

Current user's wallet.

**Response** `200`

```json
{
  "id": 1,
  "balance": 10000.0000,
  "updatedAt": "2026-06-30T10:00:00Z"
}
```

---

### POST `/wallet/add-funds`

Top up wallet (demo / test convenience).

**Request**

```json
{
  "amount": 5000
}
```

**Response** `200` — updated wallet + transaction id.

---

### GET `/wallet/transactions`

Paginated audit log.

**Query params**: `page` (default 0), `size` (default 20)

**Response** `200`

```json
{
  "content": [
    {
      "id": 10,
      "type": "DEBIT",
      "amount": 2000.0000,
      "referenceType": "ESCROW_LOCK",
      "referenceId": 3,
      "balanceAfter": 8000.0000,
      "createdAt": "2026-06-30T11:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

## Projects

### POST `/projects`

Client creates a project with milestones.

**Request**

```json
{
  "title": "Website redesign",
  "description": "Three-phase delivery",
  "milestones": [
    { "title": "Wireframes", "description": "Figma files", "amount": 5000 },
    { "title": "Implementation", "description": "React app", "amount": 15000 }
  ]
}
```

**Response** `201` — project with nested milestones (all `PENDING`).

---

### GET `/projects`

List projects for current user.

- **Client**: projects where `client_id = me`
- **Freelancer**: projects where `freelancer_id = me` or `status = OPEN` (for discovery)

**Query params**: `status` (optional filter)

**Response** `200` — array of project summaries.

---

### GET `/projects/{id}`

Project detail with milestones.

**Response** `200`

```json
{
  "id": 1,
  "title": "Website redesign",
  "status": "IN_PROGRESS",
  "client": { "id": 1, "name": "Jane Client" },
  "freelancer": { "id": 2, "name": "Bob Dev" },
  "milestones": [
    {
      "id": 1,
      "title": "Wireframes",
      "amount": 5000,
      "status": "FUNDS_LOCKED"
    }
  ]
}
```

---

### POST `/projects/{id}/accept`

Freelancer accepts an `OPEN` project.

**Response** `200` — project with `status: IN_PROGRESS`, `freelancer_id` set.

---

## Milestones

### POST `/milestones/{id}/lock-funds`

Client locks escrow for a `PENDING` milestone.

**Headers**: `Idempotency-Key: <uuid>` (required)

**Response** `200`

```json
{
  "milestoneId": 1,
  "status": "FUNDS_LOCKED",
  "escrowHoldId": 3,
  "walletBalance": 5000.0000
}
```

**Errors**

| Status | When |
|--------|------|
| 400 | Invalid state, insufficient balance |
| 409 | Wallet lock not acquired |
| 401 | Not authenticated / not project client |

---

### POST `/milestones/{id}/submit`

Freelancer submits work.

**Request**

```json
{
  "note": "Wireframes delivered. Link: https://figma.com/..."
}
```

**Response** `200` — milestone with `status: SUBMITTED`.

---

### POST `/milestones/{id}/approve`

Client approves submitted work; releases funds to freelancer.

**Response** `200`

```json
{
  "milestoneId": 1,
  "status": "APPROVED",
  "escrowHoldStatus": "RELEASED"
}
```

---

### POST `/milestones/{id}/dispute`

Client or assigned freelancer disputes submitted work. **Funds stay frozen** in escrow (`HELD`); milestone becomes `DISPUTED`. An admin must resolve it.

**Request**

```json
{
  "reason": "Deliverables do not match spec"
}
```

`reason` is required.

**Response** `200`

```json
{
  "milestoneId": 1,
  "status": "DISPUTED",
  "escrowHoldStatus": "HELD"
}
```

---

## Admin

Admin routes require `Authorization: Bearer <jwt>` and role `ADMIN` or `SUPER_ADMIN`.
Non-admins receive `403 FORBIDDEN`. Admins have **no wallet** — `/wallet` endpoints return 403 for admin roles.

### POST `/admin/admins`

Create a new `ADMIN` account. **Super admin only.** No wallet is created. `createdBy` is set to the current super admin.

**Request**

```json
{
  "name": "Ops Admin",
  "email": "admin@example.com",
  "password": "securePassword123"
}
```

**Response** `201`

```json
{
  "id": 2,
  "name": "Ops Admin",
  "email": "admin@example.com",
  "role": "ADMIN",
  "createdAt": "2026-07-31T12:00:00Z",
  "createdById": 1,
  "createdByName": "Super Admin"
}
```

**Errors**

| Status | When |
|--------|------|
| 403 | Caller is not `SUPER_ADMIN` |
| 409 | Email already registered |

---

### GET `/admin/admins`

List all `ADMIN` and `SUPER_ADMIN` users (includes who created each).

**Response** `200` — array of admin user objects (same shape as create response).

---

### GET `/admin/dashboard`

Platform stats for the admin dashboard.

**Response** `200`

```json
{
  "totalUsers": 42,
  "clients": 20,
  "freelancers": 15,
  "both": 5,
  "admins": 2,
  "openProjects": 8,
  "inProgressProjects": 12,
  "completedProjects": 10,
  "cancelledProjects": 1,
  "totalEscrowHeld": 125000.0000,
  "disputedMilestones": 3
}
```

`disputedMilestones` = count of **open** disputes.

---

### GET `/admin/disputes`

List disputes. Optional filter: `?status=OPEN` or `?status=RESOLVED`.

**Response** `200` — array of dispute objects (see resolve response shape).

---

### GET `/admin/disputes/{id}`

Dispute detail including project parties, submitted work note, and escrow status.

---

### POST `/admin/disputes/{id}/resolve`

Admin decides the dispute. Money moves only here:

| `decision` | Effect |
|------------|--------|
| `FREELANCER_WINS` | Escrow released to freelancer; milestone `APPROVED` |
| `CLIENT_WINS` | Escrow refunded to client; milestone `REFUNDED` |

**Request**

```json
{
  "decision": "FREELANCER_WINS",
  "note": "Deliverable matches the milestone scope"
}
```

**Response** `200` — updated dispute object with `status: RESOLVED` and resolution fields set.

---

## Error format

```json
{
  "error": "INVALID_MILESTONE_STATE",
  "message": "Cannot approve milestone in status PENDING",
  "timestamp": "2026-06-30T12:00:00Z"
}
```

Common error codes:

| Code | HTTP |
|------|------|
| `INVALID_MILESTONE_STATE` | 400 |
| `INSUFFICIENT_BALANCE` | 400 |
| `WALLET_BUSY` | 409 |
| `IDEMPOTENCY_KEY_CONFLICT` | 409 |
| `INVALID_CURRENT_PASSWORD` | 401 |
| `CONCURRENT_MODIFICATION` | 409 |
| `INVALID_REQUEST` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |
| `RATE_LIMIT_EXCEEDED` | 429 |

---

## Endpoint checklist

| # | Method | Path | Role |
|---|--------|------|------|
| 1 | POST | `/auth/signup` | Public |
| 2 | POST | `/auth/login` | Public |
| 3 | POST | `/auth/change-password` | Auth |
| 4 | GET | `/wallet` | Auth |
| 5 | POST | `/wallet/add-funds` | Auth |
| 6 | GET | `/wallet/transactions` | Auth |
| 7 | POST | `/projects` | Client |
| 8 | GET | `/projects` | Auth |
| 9 | GET | `/projects/{id}` | Auth |
| 10 | POST | `/projects/{id}/accept` | Freelancer |
| 11 | POST | `/milestones/{id}/lock-funds` | Client |
| 12 | POST | `/milestones/{id}/submit` | Freelancer |
| 13 | POST | `/milestones/{id}/approve` | Client |
| 14 | POST | `/milestones/{id}/dispute` | Client / Freelancer |
| 15 | POST | `/admin/admins` | Super admin |
| 16 | GET | `/admin/admins` | Admin |
| 17 | GET | `/admin/dashboard` | Admin |
| 18 | GET | `/admin/disputes` | Admin |
| 19 | GET | `/admin/disputes/{id}` | Admin |
| 20 | POST | `/admin/disputes/{id}/resolve` | Admin |
