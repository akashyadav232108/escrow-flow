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

Client disputes submitted work; refunds locked funds.

**Request** (optional)

```json
{
  "reason": "Deliverables do not match spec"
}
```

**Response** `200` — milestone `DISPUTED` or `REFUNDED`, hold `REFUNDED`.

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
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |
| `RATE_LIMIT_EXCEEDED` | 429 |

---

## Endpoint checklist

| # | Method | Path | Role |
|---|--------|------|------|
| 1 | POST | `/auth/signup` | Public |
| 2 | POST | `/auth/login` | Public |
| 3 | GET | `/wallet` | Auth |
| 4 | POST | `/wallet/add-funds` | Auth |
| 5 | GET | `/wallet/transactions` | Auth |
| 6 | POST | `/projects` | Client |
| 7 | GET | `/projects` | Auth |
| 8 | GET | `/projects/{id}` | Auth |
| 9 | POST | `/projects/{id}/accept` | Freelancer |
| 10 | POST | `/milestones/{id}/lock-funds` | Client |
| 11 | POST | `/milestones/{id}/submit` | Freelancer |
| 12 | POST | `/milestones/{id}/approve` | Client |
| 13 | POST | `/milestones/{id}/dispute` | Client |
