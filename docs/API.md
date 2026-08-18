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
    "role": "CLIENT",
    "accountStatus": "ACTIVE"
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

Also creates `PROJECT_CREATED` notifications for active freelancers (`FREELANCER` / `BOTH`), capped for demo fan-out.

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

**Legacy** — Freelancer instantly accepts an `OPEN` project (first-come). Prefer the applications flow below for hiring.

**Response** `200` — project with `status: IN_PROGRESS`, `freelancer_id` set.

---

## Project applications

Freelancers apply to `OPEN` projects; the client accepts one or declines. Rows share the business transaction (no Kafka).

### POST `/projects/{projectId}/applications`

Freelancer applies. Optional body:

```json
{ "message": "I can start this week." }
```

**Response** `201`

```json
{
  "id": 1,
  "projectId": 5,
  "projectTitle": "Website redesign",
  "freelancerId": 2,
  "freelancerName": "Bob Dev",
  "status": "PENDING",
  "message": "I can start this week.",
  "createdAt": "2026-08-03T12:00:00Z",
  "updatedAt": "2026-08-03T12:00:00Z"
}
```

Also notifies the project client (`APPLICATION_RECEIVED`).

### GET `/projects/{projectId}/applications`

Client (or assigned freelancer) lists applications for the project.

**Response** `200` — array of `ApplicationResponse`.

### GET `/applications/mine`

Current freelancer's applications (newest first).

### POST `/applications/{applicationId}/accept`

Client accepts a `PENDING` application → project `IN_PROGRESS`, other pending applications declined, and creates a **project agreement** with the client already marked accepted. Notifies freelancer (`APPLICATION_ACCEPTED`).

**Body** (required):

```json
{ "acceptedTerms": true }
```

`acceptedTerms` must be `true` (`@AssertTrue`). Freelancer must still accept the agreement before lock/submit/approve/dispute.

### POST `/applications/{applicationId}/decline`

Client declines a `PENDING` application. Notifies freelancer (`APPLICATION_DECLINED`).

### POST `/applications/{applicationId}/withdraw`

Freelancer withdraws their own `PENDING` application.

---

## Project agreements (hire-time terms)

Shared terms created when the client accepts an application. **No automatic penalties** — acknowledgement only; used as evidence in disputes/exits. Milestone lock, submit, approve, and dispute stay blocked until **both** parties have accepted. Legacy projects without an agreement row are allowed to continue.

### GET `/projects/{projectId}/agreement`

Client, assigned freelancer, or admin. `404` if none.

**Response** `200`:

```json
{
  "id": 1,
  "projectId": 10,
  "termsVersion": "1.0",
  "termsText": "...",
  "clientAcceptedAt": "2026-08-17T06:00:00Z",
  "freelancerAcceptedAt": null,
  "clientAccepted": true,
  "freelancerAccepted": false,
  "fullyAccepted": false,
  "createdAt": "2026-08-17T06:00:00Z"
}
```

### POST `/projects/{projectId}/agreement/accept`

Client or assigned freelancer records their acceptance (idempotent per party once). Admins cannot accept.

---

## Project exits (Phase B)

Client or assigned freelancer can request to end an `IN_PROGRESS` project. Project becomes `EXIT_DISPUTED` (milestone actions frozen). Admin settles each **held** escrow milestone by choosing how much goes to the freelancer (rest refunds to client), then **cancels** or **reopens** the project. Admin decision is final.

### POST `/projects/{projectId}/exit`

Body: `{ "reason": "..." }` → `201` `ProjectExitResponse`.

### GET `/projects/{projectId}/exit`

Open exit for the project (parties only).

### GET `/project-exits/{exitId}`

Exit detail (party or admin).

### Admin

- `GET /admin/project-exits?status=`
- `GET /admin/project-exits/{id}`
- `POST /admin/project-exits/{id}/resolve`

```json
{
  "projectOutcome": "CANCELLED",
  "adminNote": "Partial work credited",
  "settlements": [
    { "milestoneId": 12, "freelancerAmount": 2000.0000 }
  ]
}
```

`freelancerAmount` must be between `0` and the snapshotted `holdAmount`. Client refund = hold − freelancer. Full freelancer → hold `RELEASED` / milestone `APPROVED`; full client → `REFUNDED`; both &gt; 0 → hold `SPLIT` / milestone `SETTLED`.

---

## Milestones

### POST `/milestones/{id}/lock-funds`

Client locks escrow for a `PENDING` milestone, or **re-locks** a `REFUNDED` milestone (reuses the same `escrow_holds` row: `REFUNDED` → `HELD`; wallet history kept via new `ESCROW_LOCK` debit).

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

Also creates an in-app notification for the project client (`WORK_SUBMITTED`).

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

Also notifies the other party and admins (`DISPUTE_RAISED`).

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
  "warnedUsers": 3,
  "suspendedUsers": 1,
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

Also notifies the project client and freelancer (`DISPUTE_RESOLVED`).

---

### GET `/admin/users`

List marketplace users (`CLIENT` / `FREELANCER` / `BOTH`). Optional `?status=WARNED|SUSPENDED|ACTIVE|DELETED`.
When `status` is omitted, deleted users are excluded.

---

### GET `/admin/users/{id}`

User detail including warning history.

---

### POST `/admin/users/{id}/warnings`

Issue a warning. If the user is `ACTIVE`, status becomes `WARNED`.

**Request**

```json
{
  "reason": "Abusive messaging toward freelancer"
}
```

---

### POST `/admin/users/{id}/suspend`

Suspend account (blocks login and JWT use). Requires `reason`.

---

### POST `/admin/users/{id}/unsuspend`

Restore suspended user to `WARNED` (if they have warnings) or `ACTIVE`.

---

### POST `/admin/users/{id}/delete`

Soft-delete account (`DELETED` + `deletedAt`). Blocked if the user has open disputes, held escrow, or in-progress projects.

**Request**

```json
{
  "reason": "Repeated policy violations after warnings"
}
```

---

## Notifications

In-app notifications for the authenticated user. Rows are written in the same DB transaction as the business action (no Kafka). Admins may receive dispute alerts even though they have no wallet.

### GET `/notifications`

Paginated list for the current user (newest first).

**Query params**: `page` (default 0), `size` (default 20)

**Response** `200`

```json
{
  "content": [
    {
      "id": 1,
      "type": "WORK_SUBMITTED",
      "title": "Work submitted",
      "message": "Freelancer submitted work for milestone \"Wireframes\" on project \"Website redesign\".",
      "referenceType": "PROJECT",
      "referenceId": 12,
      "read": false,
      "createdAt": "2026-07-31T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`type`: `PROJECT_CREATED` | `WORK_SUBMITTED` | `DISPUTE_RAISED` | `DISPUTE_RESOLVED` | `REVIEW_RECEIVED` | `APPLICATION_RECEIVED` | `APPLICATION_ACCEPTED` | `APPLICATION_DECLINED` | `PROJECT_EXIT_RAISED` | `PROJECT_EXIT_RESOLVED`  
`referenceType`: `PROJECT` | `MILESTONE` | `DISPUTE` | `PROJECT_EXIT` (nullable)

### GET `/notifications/unread-count`

**Response** `200`

```json
{
  "unreadCount": 3
}
```

### POST `/notifications/{id}/read`

Mark one notification as read. Only the recipient may update their own row (`404` if missing or not owned).

**Response** `200` — updated `NotificationResponse`.

### POST `/notifications/read-all`

Mark all of the current user's unread notifications as read.

**Response** `200`

```json
{
  "unreadCount": 0
}
```

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
| `ACCOUNT_SUSPENDED` | 403 |
| `ACCOUNT_DELETED` | 403 |
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
| 10 | POST | `/projects/{id}/accept` | Freelancer (legacy) |
| 10a | POST | `/projects/{id}/applications` | Freelancer |
| 10b | GET | `/projects/{id}/applications` | Client |
| 10c | GET | `/applications/mine` | Freelancer |
| 10d | POST | `/applications/{id}/accept` | Client (body: acceptedTerms) |
| 10e | POST | `/applications/{id}/decline` | Client |
| 10f | POST | `/applications/{id}/withdraw` | Freelancer |
| 10g | GET | `/projects/{id}/agreement` | Client / freelancer / admin |
| 10h | POST | `/projects/{id}/agreement/accept` | Client / freelancer |
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
| 21 | GET | `/admin/users` | Admin |
| 22 | GET | `/admin/users/{id}` | Admin |
| 23 | POST | `/admin/users/{id}/warnings` | Admin |
| 24 | POST | `/admin/users/{id}/suspend` | Admin |
| 25 | POST | `/admin/users/{id}/unsuspend` | Admin |
| 26 | POST | `/admin/users/{id}/delete` | Admin |
| 27 | GET | `/notifications` | Auth |
| 28 | GET | `/notifications/unread-count` | Auth |
| 29 | POST | `/notifications/{id}/read` | Auth |
| 30 | POST | `/notifications/read-all` | Auth |
