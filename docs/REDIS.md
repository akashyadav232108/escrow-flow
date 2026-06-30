# Redis design

Redis is used for **correctness and safety**, not performance caching. Three concrete use cases — each justified in an interview.

## Overview

| Use case | Key pattern | TTL | Why |
|----------|-------------|-----|-----|
| Wallet lock | `lock:wallet:{walletId}` | 5s (PX) | Prevent concurrent debits |
| Idempotency | `idem:{uuid}` | 24h | Stop double lock on retry |
| Dispute rate limit | `ratelimit:dispute:{userId}` | 24h | Abuse prevention (optional) |

## 1. Distributed wallet lock

### Problem

Two simultaneous requests can read the same wallet balance and both debit successfully, driving balance negative or double-spending.

### Solution

Serialize all wallet mutations per `walletId` using an atomic lock.

### Key

```text
lock:wallet:{walletId}
```

### Acquire

```redis
SET lock:wallet:42 <requestId> NX PX 5000
```

- `NX` — set only if key does not exist (atomic)
- `PX 5000` — auto-expire after 5 seconds (safety net if process crashes before release)
- `<requestId>` — random UUID per attempt; used for safe release

### Release (Lua script)

Only delete if the value matches — prevents deleting another process's lock after yours expired:

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

### Usage in Spring

```text
1. Try acquire lock (retry 2-3× with short backoff, or fail fast)
2. BEGIN DB transaction
3. Read wallet, validate balance, debit/credit, insert audit row
4. COMMIT (or ROLLBACK on error)
5. Release lock in finally block (after commit/rollback)
```

### On lock failure

Return HTTP `409 Conflict` with message like `"Wallet busy, please retry"` — client can retry with same idempotency key for lock-funds.

---

## 2. Idempotency keys (lock funds)

### Problem

Users double-click "Lock Funds". Mobile clients retry on timeout. Without idempotency, a retried request debits twice.

### Solution

Client sends `Idempotency-Key: <uuid>` header. Server stores processed response in Redis.

### Key

```text
idem:{clientGeneratedUUID}
```

### Flow

```text
1. Read idem:{key}
   → if exists: deserialize cached JSON response, return 200 (same status/body)

2. Process lock-funds normally (with wallet lock + DB transaction)

3. SETEX idem:{key} 86400 <serialized response>
```

### Frontend

Generate UUID when user clicks "Lock Funds" — **before** the API call:

```javascript
const idempotencyKey = crypto.randomUUID();
await api.post(`/milestones/${id}/lock-funds`, body, {
  headers: { 'Idempotency-Key': idempotencyKey }
});
```

Reuse the **same** key if the client retries the same user action.

### Scope

Minimum: `POST /milestones/{id}/lock-funds`.

Optional later: approve, dispute, add-funds.

---

## 3. Dispute rate limiting (optional)

### Problem

Spam dispute creation could abuse freelancers or flood support.

### Key

```text
ratelimit:dispute:{userId}
```

### Flow

```redis
INCR ratelimit:dispute:7
EXPIRE ratelimit:dispute:7 86400 NX   -- set TTL only on first increment
```

Reject if count > limit (e.g. 5 per day) with HTTP `429 Too Many Requests`.

---

## What we do NOT use Redis for

| Anti-pattern | Why skip |
|--------------|----------|
| Cache project lists | MySQL is fast enough; stale cache invalidation adds complexity |
| Cache wallet balance | Balance must come from DB after mutations |
| Session store | JWT is stateless for v1; optional JWT blacklist on logout only |

Padding the design with generic caching dilutes the correctness story in interviews.

---

## Spring configuration sketch

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Beans:

- `StringRedisTemplate` or `RedisTemplate<String, String>`
- `WalletLockService` — acquire / release with Lua
- `IdempotencyService` — get / put cached responses

---

## Failure modes

| Scenario | Behavior |
|----------|----------|
| Redis down during lock | Fail request (do not proceed without lock) — money safety > availability |
| Lock TTL expires mid-transaction | Keep transactions short (< 1s); `@Version` catches stray writes |
| Idempotency key collision (UUID) | Negligible; UUID v4 is safe |
| Cached idempotency response lost | Worst case: duplicate processing if client retries after 24h — acceptable for demo |

---

## Testing

- Integration test: two parallel lock-funds requests → only one succeeds debiting
- Integration test: same idempotency key twice → same response, single debit
- Unit test: Lua release only deletes matching value
