# Backend implementation guide

Spring Boot 3 monolith in `/backend`. MySQL for persistence, Redis for locks and idempotency.

## Tech stack

- Java 17+
- Spring Boot 3.x (Web, Data JPA, Security, Validation, Data Redis)
- MySQL 8 + Flyway migrations
- Redis 7 (Lettuce client via Spring Data Redis)
- JWT (jjwt or Spring Security OAuth2 Resource Server pattern)
- Maven

## Project scaffold

Generate via [start.spring.io](https://start.spring.io) or manually:

**Dependencies**: Spring Web, Spring Data JPA, MySQL Driver, Spring Data Redis, Spring Security, Validation, Flyway, Lombok (optional)

```
backend/
├── pom.xml
├── src/main/java/com/escrowflow/
│   ├── EscrowFlowApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── RedisConfig.java
│   │   └── JwtConfig.java
│   ├── domain/
│   │   ├── User.java
│   │   ├── Wallet.java
│   │   ├── Project.java
│   │   ├── Milestone.java
│   │   ├── EscrowHold.java
│   │   └── WalletTransaction.java
│   ├── repository/
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── WalletService.java
│   │   ├── ProjectService.java
│   │   ├── MilestoneService.java
│   │   └── EscrowService.java
│   ├── infrastructure/
│   │   ├── RedisWalletLockService.java
│   │   └── IdempotencyService.java
│   └── web/
│       ├── AuthController.java
│       ├── WalletController.java
│       ├── ProjectController.java
│       ├── MilestoneController.java
│       ├── dto/
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           └── InvalidMilestoneStateException.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-example.yml
│   └── db/migration/
│       └── V1__init_schema.sql
└── src/test/java/
```

## Configuration

`application-example.yml` (copy to `application-local.yml`, gitignored):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/escrow_flow
    username: escrow
    password: changeme
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
  data:
    redis:
      host: localhost
      port: 6379

app:
  jwt:
    secret: change-this-to-a-long-random-string
    expiration-ms: 86400000
  wallet:
    starting-balance: 10000
```

---

## Phase 1: Auth + wallet (Week 1–2)

### Tasks

- [ ] Create Flyway `V1__init_schema.sql` with `users`, `wallets`, `wallet_transactions`
- [ ] `User` entity + `UserRepository`
- [ ] `Wallet` entity with `@Version` on `version` field
- [ ] `WalletTransaction` entity (append-only)
- [ ] `AuthService.signup()` — create user + wallet in one `@Transactional`
- [ ] `AuthService.login()` — verify password, issue JWT
- [ ] `SecurityConfig` — permit `/api/auth/**`, secure everything else
- [ ] `WalletService.addFunds()` — credit balance + insert transaction row
- [ ] `WalletService.getBalance()` — read wallet for current user
- [ ] `WalletController` — GET `/wallet`, POST `/wallet/add-funds`

### Wallet debit helper (used later by escrow)

```java
@Transactional
public Wallet debit(Wallet wallet, BigDecimal amount, ReferenceType ref, Long refId) {
    if (wallet.getBalance().compareTo(amount) < 0) {
        throw new InsufficientBalanceException();
    }
    wallet.setBalance(wallet.getBalance().subtract(amount));
    walletRepository.save(wallet); // @Version checked here
    transactionRepository.save(WalletTransaction.debit(wallet, amount, ref, refId));
    return wallet;
}
```

### Tests

- Signup creates user + wallet with starting balance
- Add funds increases balance and creates one CREDIT transaction
- Balance invariant: sum(transactions) == balance

---

## Phase 2: Projects + milestones (Week 3–4)

### Tasks

- [ ] Flyway `V2__projects_milestones.sql`
- [ ] `Project`, `Milestone` entities
- [ ] `ProjectService.create()` — client only, nested milestones as `PENDING`
- [ ] `ProjectService.accept()` — freelancer sets self on project, `IN_PROGRESS`
- [ ] `ProjectService.listForUser()` — role-aware query
- [ ] Controllers for create, list, get, accept
- [ ] Authorization: only client can create; only assigned freelancer can accept

### No money movement in this phase

Validate milestone CRUD and project lifecycle before touching escrow.

---

## Phase 3: Escrow core (Week 5)

### Tasks

- [ ] Flyway `V3__escrow_holds.sql`
- [ ] `EscrowHold` entity
- [ ] `RedisWalletLockService` — see [REDIS.md](REDIS.md)
- [ ] `EscrowService.lockFunds(milestoneId, clientId)`:
  1. Acquire Redis lock on client wallet
  2. `@Transactional`: validate state `PENDING`, debit, insert hold `HELD`, log `ESCROW_LOCK`, milestone → `FUNDS_LOCKED`
  3. Release lock in `finally`
- [ ] `EscrowService.approve()` — release to freelancer wallet
- [ ] `EscrowService.dispute()` — refund client wallet
- [ ] `MilestoneService.submit()` — `FUNDS_LOCKED` → `SUBMITTED`
- [ ] State checks in service layer — `InvalidMilestoneStateException`

### Approve flow (pseudocode)

```java
@Transactional
public void approve(Long milestoneId, Long clientId) {
    Milestone m = loadAndAuthorize(milestoneId, clientId);
    if (m.getStatus() != SUBMITTED) throw new InvalidMilestoneStateException();
    EscrowHold hold = holdRepo.findByMilestoneId(m.getId()).orElseThrow();
    hold.setStatus(RELEASED);
    hold.setResolvedAt(now());
    Wallet freelancerWallet = walletRepo.findByUserId(m.getProject().getFreelancerId());
    walletService.credit(freelancerWallet, hold.getAmount(), ESCROW_RELEASE, hold.getId());
    m.setStatus(APPROVED);
}
```

### Tests

- Full lifecycle: lock → submit → approve (balances correct on both wallets)
- Dispute refunds client
- Illegal transition throws 400

---

## Phase 4: Idempotency + audit (Week 6)

### Tasks

- [ ] `IdempotencyService` — Redis GET/SETEX cached responses
- [ ] `IdempotencyFilter` or controller advice on lock-funds
- [ ] `GET /wallet/transactions` with pagination
- [ ] `WalletConsistencyTest` — assert balance == sum(transactions) for all wallets
- [ ] Optional: dispute rate limiter

---

## Cross-cutting concerns

### Exception handling

`GlobalExceptionHandler` maps:

- `InvalidMilestoneStateException` → 400
- `InsufficientBalanceException` → 400
- `OptimisticLockException` → 409
- `WalletLockException` → 409

### Logging

Log at INFO: escrow state transitions with milestone id, amounts, user ids (no passwords).

### Security

- BCrypt password encoding
- JWT claims: `userId`, `email`, `role`
- Method-level checks: `@PreAuthorize` or manual checks in services

---

## Dependencies (pom.xml sketch)

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
  </dependency>
</dependencies>
```

---

## Run locally

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Ensure MySQL and Redis are running before start.
