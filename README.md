# Movie Ticket Booking Application

A production-ready REST API for booking movie tickets — theatres, screens, shows, seats, bookings and payments.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Domain Model](#domain-model)
- [API Reference](#api-reference)
- [Authentication](#authentication)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run Locally (Docker)](#run-locally-docker)
  - [Run Locally (Maven)](#run-locally-maven)
- [Configuration](#configuration)
- [Database](#database)
- [Testing](#testing)
- [Observability](#observability)
- [CI/CD](#cicd)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 (H2 for tests) |
| Migrations | Flyway |
| Security | Spring Security + JWT (JJWT 0.12.7) |
| API Docs | SpringDoc OpenAPI 2.8.17 (Swagger UI) |
| Build | Maven 3.9+ |
| Containerisation | Docker (multi-stage), Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers 1.21.4 |
| Coverage | JaCoCo 0.8.12 (≥85% enforced) |
| CI | GitHub Actions |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST Controllers                      │
│   Auth · Theatre · Screen · Show · Booking · Payment    │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                    Service Layer                         │
│  UserService · TheatreService · ScreenService           │
│  ShowService · BookingService · PaymentService          │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              Spring Data JPA Repositories               │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                    PostgreSQL                            │
└─────────────────────────────────────────────────────────┘
```

**Key design decisions:**

- **Pessimistic DB-level locking** on `ShowSeat` rows during booking prevents double-booking under concurrent load across multiple nodes — replacing the JVM-level `synchronized` approach that breaks in distributed deployments.
- **Stateless JWT authentication** — no server-side sessions; scales horizontally.
- **Flyway migrations** — schema changes are versioned and auditable.
- **Optimistic locking** (`@Version`) on `ShowSeat` as a secondary safety net.
- **Request-scoped correlation IDs** injected into MDC for distributed tracing.

---

## Project Structure

```
src/
├── main/java/com/rsh/mtba/
│   ├── App.java                        # Spring Boot entry point
│   ├── config/
│   │   ├── SecurityConfig.java         # Spring Security + JWT filter chain
│   │   └── OpenApiConfig.java          # Swagger / OpenAPI configuration
│   ├── controller/                     # REST controllers (thin — delegate to services)
│   │   ├── AuthController.java
│   │   ├── TheatreController.java
│   │   ├── ScreenController.java
│   │   ├── ShowController.java
│   │   ├── BookingController.java
│   │   ├── PaymentController.java
│   │   └── UserController.java
│   ├── dto/
│   │   ├── request/                    # Validated inbound request DTOs
│   │   └── response/                   # Outbound response DTOs (never expose entities)
│   ├── entity/                         # JPA entities
│   │   ├── User.java
│   │   ├── Theatre.java
│   │   ├── Screen.java
│   │   ├── Seat.java
│   │   ├── Show.java
│   │   ├── ShowSeat.java
│   │   ├── Booking.java
│   │   └── Payment.java
│   ├── exception/                      # Custom exceptions + global handler
│   │   └── GlobalExceptionHandler.java
│   ├── repository/                     # Spring Data JPA repos
│   ├── security/
│   │   ├── JwtUtil.java
│   │   ├── JwtAuthFilter.java          # Reads Bearer token, sets SecurityContext + MDC
│   │   └── UserDetailsServiceImpl.java
│   └── service/                        # Business logic
│       ├── UserService.java
│       ├── TheatreService.java
│       ├── ScreenService.java
│       ├── ShowService.java
│       ├── BookingService.java
│       └── PaymentService.java
├── main/resources/
│   ├── application.yml                 # Base config
│   ├── application-dev.yml             # Dev overrides (local PostgreSQL)
│   ├── application-prod.yml            # Prod overrides (env-var secrets, pool tuning)
│   ├── logback-spring.xml              # Human-readable in dev, JSON in prod
│   └── db/migration/
│       └── V1__init_schema.sql         # Full DDL with indexes
└── test/java/com/rsh/mtba/
    ├── functional/
    │   └── BookingFlowFunctionalTest.java   # 26 real HTTP tests (RANDOM_PORT)
    ├── controller/                          # MockMvc integration tests
    └── service/                             # Mockito unit tests
```

---

## Domain Model

```
Theatre ──< Screen ──< Seat
                  └──< Show ──< ShowSeat >──< Booking >── Payment
                                                └── User
```

| Entity | Key fields |
|---|---|
| `User` | `email` (unique), `passwordHash`, `gender`, `role` (USER/ADMIN) |
| `Theatre` | `name`, `address`, `city` |
| `Screen` | `name`, `rows`, `cols`, `totalCapacity`, FK→Theatre |
| `Seat` | `label` (e.g. A1), `rowNumber`, `colNumber`, `type` (REGULAR/PREMIUM/RECLINER/BLOCKED) |
| `Show` | `movieName`, `startTime`, `endTime`, `basePriceInPaise`, FK→Screen |
| `ShowSeat` | `status` (AVAILABLE/LOCKED/BOOKED), `@Version` (optimistic lock), FK→Show+Seat |
| `Booking` | `totalAmountInPaise`, `status` (PROCESSING→PAYMENT_INITIATED→COMPLETED/CANCELLED), FK→User+Show |
| `Payment` | `transactionId`, `amountInPaise`, `status` (INITIATED→COMPLETED/FAILED), FK→Booking |

### Booking state machine

```
                 book()
AVAILABLE ──────────────► LOCKED
                             │
              initiatePayment()│
                             ▼
                     PAYMENT_INITIATED
                        /        \
        confirmPayment()          failPayment()
                /                        \
           COMPLETED                  PAYMENT_FAILED
           (seats→BOOKED)           (seats→AVAILABLE)

cancelBooking() from PROCESSING or PAYMENT_INITIATED → CANCELLED (seats→AVAILABLE)
```

---

## API Reference

All endpoints are prefixed with `/api`. Full interactive docs are available at `/swagger-ui.html` when the app is running.

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | Public | Register a new user |
| `POST` | `/api/auth/login` | Public | Login — returns JWT token |

### Theatres

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/theatres` | Public | List all theatres (filter by `?city=`) |
| `GET` | `/api/theatres/{id}` | Public | Get theatre by ID |
| `POST` | `/api/theatres` | Admin | Create theatre |
| `PUT` | `/api/theatres/{id}` | Admin | Update theatre |
| `DELETE` | `/api/theatres/{id}` | Admin | Delete theatre |

### Screens

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/theatres/{id}/screens` | Admin | Add screen to theatre (auto-generates seats) |
| `GET` | `/api/theatres/{id}/screens` | Public | List screens for a theatre |
| `GET` | `/api/screens/{id}` | Public | Get screen by ID |

### Shows

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/screens/{id}/shows` | Admin | Schedule a show (auto-generates ShowSeats) |
| `GET` | `/api/shows` | Public | List upcoming shows |
| `GET` | `/api/shows/{id}` | Public | Get show by ID (includes available seat count) |
| `GET` | `/api/shows/{id}/seats` | Public | Get full seat map with AVAILABLE/LOCKED/BOOKED status |

### Bookings

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/bookings` | User | Book seats (acquires pessimistic DB lock) |
| `GET` | `/api/bookings/{id}` | User | Get booking by ID |
| `GET` | `/api/bookings/my` | User | Get all bookings for authenticated user |
| `DELETE` | `/api/bookings/{id}` | User | Cancel booking (releases seats back to AVAILABLE) |

### Payments

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/payments` | User | Initiate payment for a booking |
| `POST` | `/api/payments/confirm/{transactionId}` | Any | Confirm payment (simulates gateway callback) |
| `POST` | `/api/payments/fail/{transactionId}` | Any | Fail payment (simulates gateway failure callback) |
| `GET` | `/api/payments/{id}` | User | Get payment by ID |
| `GET` | `/api/payments/booking/{bookingId}` | User | Get payment for a booking |

### Users

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/users/me` | User | Get authenticated user's profile |
| `GET` | `/api/users/{id}` | Admin | Get any user by ID |

### Error responses

All errors follow a consistent JSON structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Theatre not found with id: 99",
  "path": "/api/theatres/99",
  "timestamp": "2026-07-06T00:00:00",
  "validationErrors": { "name": "Name is required" }
}
```

| HTTP Status | When |
|---|---|
| `400` | Validation failure or invalid state transition |
| `401` | Bad credentials on login |
| `402` | Payment processing error |
| `403` | Missing or invalid JWT, or insufficient role |
| `404` | Resource not found |
| `409` | Seat not available (concurrent booking conflict) or duplicate email |
| `500` | Unexpected server error |

---

## Authentication

The API uses **stateless JWT Bearer tokens**.

**1. Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"secret123","gender":"FEMALE"}'
```

**2. Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123"}'
# → { "token": "eyJ...", "tokenType": "Bearer", "user": { ... } }
```

**3. Use the token:**
```bash
curl http://localhost:8080/api/bookings/my \
  -H "Authorization: Bearer eyJ..."
```

Tokens expire after **24 hours** by default (configurable via `JWT_EXPIRATION_MS`).

**Roles:**
- `ROLE_USER` — register, login, browse shows/seats, book tickets, pay, cancel own bookings
- `ROLE_ADMIN` — everything above + create/update/delete theatres, screens, shows

---

## Getting Started

### Prerequisites

- Java 21+
- Docker + Docker Compose (for the quickest start)
- Maven 3.9+ (for local builds without Docker)

### Run Locally (Docker)

```bash
# Clone
git clone https://github.com/rahils/movie-ticket-booking-application.git
cd movie-ticket-booking-application

# Start PostgreSQL + application
docker compose up --build

# App is available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

### Run Locally (Maven)

**Step 1 — Start PostgreSQL:**
```bash
docker run -d \
  --name mtba-postgres \
  -e POSTGRES_DB=mtba_dev \
  -e POSTGRES_USER=mtba \
  -e POSTGRES_PASSWORD=mtba \
  -p 5432:5432 \
  postgres:16-alpine
```

**Step 2 — Build and run:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Step 3 — Create an admin user** (seed via `/api/auth/register`, then manually promote in DB):
```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'your@email.com';
```

### Quick API walkthrough

```bash
BASE=http://localhost:8080

# Register + login
curl -s -X POST $BASE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","email":"admin@example.com","password":"adminpass","gender":"MALE"}'

TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"adminpass"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Create theatre → screen → show
THEATRE_ID=$(curl -s -X POST $BASE/api/theatres \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"PVR","address":"MG Road","city":"Bangalore"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

SCREEN_ID=$(curl -s -X POST $BASE/api/theatres/$THEATRE_ID/screens \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Screen 1","rows":10,"cols":15}' | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

SHOW_ID=$(curl -s -X POST $BASE/api/screens/$SCREEN_ID/shows \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"movieName":"Inception","startTime":"2026-12-01T18:00:00","endTime":"2026-12-01T21:00:00","basePriceInPaise":25000}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

# Browse seats
curl -s $BASE/api/shows/$SHOW_ID/seats | python3 -m json.tool | head -20

# Book seats
BOOKING_ID=$(curl -s -X POST $BASE/api/bookings \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"showId\":$SHOW_ID,\"seatLabels\":[\"A1\",\"A2\"]}" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

# Pay
TXN=$(curl -s -X POST $BASE/api/payments \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"bookingId\":$BOOKING_ID}" | python3 -c "import sys,json; print(json.load(sys.stdin)['transactionId'])")

# Confirm (simulates gateway callback)
curl -s -X POST $BASE/api/payments/confirm/$TXN | python3 -m json.tool
```

---

## Configuration

| Property | Env var | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | H2 (test) / local PG (dev) | JDBC URL |
| `spring.datasource.username` | `DB_USERNAME` | `mtba` | DB user |
| `spring.datasource.password` | `DB_PASSWORD` | `mtba` | DB password |
| `app.jwt.secret` | `JWT_SECRET` | `change-me-in-production-...` | HMAC-SHA256 signing key (≥32 chars) |
| `app.jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime in milliseconds |
| `server.port` | `PORT` | `8080` | HTTP port |

> **Never** use the default `JWT_SECRET` in production. Generate a strong secret:
> ```bash
> openssl rand -base64 48
> ```

Spring profiles:
- `dev` — local PostgreSQL, verbose SQL logging, Flyway baseline-on-migrate
- `prod` — all config from env vars, no SQL logging, HikariCP pool size 20
- `test` — H2 in-memory, Flyway disabled, Hibernate creates schema via `ddl-auto`

---

## Database

Schema is managed by **Flyway**. Migrations live in `src/main/resources/db/migration/`.

| Migration | Description |
|---|---|
| `V1__init_schema.sql` | Full DDL: all tables, foreign keys, indexes |

### Entity-relationship overview

```
users
  id, name, email (unique), password_hash, phone, gender, role, created_at

theatres
  id, name, address, city

screens
  id, name, total_capacity, rows, cols, theatre_id →theatres

seats
  id, label, row_number, col_number, type, screen_id →screens
  UNIQUE (screen_id, label)

shows
  id, movie_name, start_time, end_time, base_price_in_paise, screen_id →screens

show_seats
  id, status, version, show_id →shows, seat_id →seats
  UNIQUE (show_id, seat_id)

bookings
  id, total_amount_in_paise, status, created_at, updated_at
  user_id →users, show_id →shows

booking_show_seats          ← join table
  booking_id →bookings, show_seat_id →show_seats

payments
  id, transaction_id (unique), amount_in_paise, status, failure_reason
  created_at, completed_at, booking_id →bookings (unique)
```

---

## Testing

```bash
# Run all tests
mvn test -Dspring.profiles.active=test

# Run tests + enforce ≥85% coverage gate
mvn verify -Dspring.profiles.active=test
```

### Test suite (117 tests)

| Layer | Files | Tests | Type |
|---|---|---|---|
| Functional | `BookingFlowFunctionalTest` | 26 | Real HTTP (RANDOM_PORT + TestRestTemplate) |
| Controller | `AuthControllerIntegrationTest`, `TheatreControllerIntegrationTest`, `TheatreControllerExtendedTest`, `FullApiCoverageTest` | 56 | MockMvc (Spring context) |
| Service | `BookingServiceTest`, `BookingServiceExtendedTest`, `PaymentServiceTest`, `PaymentServiceExtendedTest`, `ShowServiceTest`, `TheatreServiceTest`, `TheatreServiceExtendedTest`, `UserServiceTest` | 43 | Mockito unit tests |
| Security | `JwtUtilTest` | 8 | Pure unit tests |

### Coverage (JaCoCo, enforced at build time)

| Metric | Coverage |
|---|---|
| Instruction | **94.3%** |
| Branch | **86.0%** |
| Line | **95.1%** |
| Method | **91.9%** |

The JaCoCo `check` goal runs on `mvn verify` and **fails the build** if instruction or branch coverage drops below 85% (excluding auto-generated Lombok code, DTOs, entities, and config classes).

Coverage report: `target/site/jacoco/index.html`

### Functional test scenario (`BookingFlowFunctionalTest`)

The functional test starts a real embedded Tomcat, calls the API over HTTP, and validates the full booking journey:

1. Register user + admin
2. Login → receive JWT tokens
3. Admin: create theatre → screen (auto-seeds seats A1–E10) → schedule show
4. User: browse upcoming shows, inspect seat map (50 AVAILABLE)
5. User: book A1 + A2 → verify seats LOCKED
6. Attempt concurrent booking of A1 → **409 Seat Not Available**
7. User: initiate payment → gateway confirms → booking **COMPLETED**, A1/A2 **BOOKED**
8. User: book B1 + B2, then cancel → seats released back to **AVAILABLE**
9. Edge cases: 404 on missing resources, 403 without auth

---

## Observability

### Health & metrics (Spring Boot Actuator)

| Endpoint | Access | Description |
|---|---|---|
| `GET /actuator/health` | Public | Liveness/readiness probe |
| `GET /actuator/info` | Public | Application info |
| `GET /actuator/metrics` | Authenticated | JVM, HTTP, HikariCP metrics |

### Logging

- **Development**: Human-readable console output with timestamp, thread, correlation ID, level, logger, message.
- **Production** (`SPRING_PROFILES_ACTIVE=prod`): Structured JSON output — each log line is a valid JSON object suitable for log aggregators (ELK, Datadog, CloudWatch).

Every request automatically gets a **correlation ID** (`X-Correlation-Id` response header + `correlationId` MDC key) for end-to-end request tracing across log lines.

```json
{"timestamp":"2026-07-06T00:00:00.000+0000","level":"INFO","correlationId":"abc-123","userEmail":"alice@example.com","logger":"BookingService","message":"Created booking id=42 userId=1 showId=7 seats=[A1, A2]"}
```

---

## CI/CD

GitHub Actions workflow (`.github/workflows/maven.yml`):

```
Push / PR to main
       │
       ▼
  build-and-test
  ├─ Setup JDK 21 (Temurin)
  ├─ mvn verify          ← builds, tests, enforces ≥85% coverage
  └─ Upload test reports (surefire-reports/)
       │
       ▼ (main branch only)
  docker-build
  ├─ Build Docker image
  └─ Push to ghcr.io/<owner>/<repo>:latest + :<sha>
```

Dependabot is configured to check Maven dependencies weekly.

### Docker image

```bash
# Pull latest image
docker pull ghcr.io/<owner>/movie-ticket-booking-application:latest

# Run with PostgreSQL
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host:5432/mtba \
  -e DB_USERNAME=mtba \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=$(openssl rand -base64 48) \
  -p 8080:8080 \
  ghcr.io/<owner>/movie-ticket-booking-application:latest
```

The image:
- Uses a **multi-stage build** (Maven build → JRE 21 Alpine runtime) — final image ~200 MB
- Runs as a **non-root user** (`appuser`)
- Has a built-in **HEALTHCHECK** (`/actuator/health`)
- Respects container memory limits (`-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`)
