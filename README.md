# Movie Ticket Booking Application

A Spring Boot reference API for theatres, screens, shows, seat inventory, bookings, and simulated payments.

> This project is intended for learning and as an implementation reference. Payment callbacks are simulated and do not integrate with a real payment gateway or process money. Review security, operational, and regulatory requirements before production use.

## Features

- Stateless JWT authentication with database-backed current roles.
- Admin-managed theatres, screens, and show schedules.
- PostgreSQL row locking and optimistic version checks for seat booking.
- Expiring seat holds with ownership-aware release.
- Owner/admin authorization for booking and payment records.
- Flyway migrations, Swagger UI, health endpoints, Docker Compose, and structured logging.
- Unit tests plus PostgreSQL/Testcontainers integration tests that call actual HTTP APIs.

## Technology

| Area | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 4.1.0 |
| Persistence | Spring JDBC with `JdbcTemplate` |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Security | Spring Security, JJWT 0.13.0 |
| API documentation | SpringDoc OpenAPI 3.0.3 |
| Testing | JUnit, Mockito, Testcontainers |
| Coverage | JaCoCo 0.8.15, instruction and branch gates at 85% |

## Architecture

```text
HTTP controllers -> services -> JDBC repositories -> PostgreSQL
```

Booking transactions lock requested `show_seats` rows. Each held seat records its current booking owner, so delayed cancellation or payment failure cannot release inventory belonging to a later booking. A scheduled worker expires abandoned holds.

## Requirements

- Docker with Compose
- Java 21 and Maven 3.9+ for local development
- `curl` for the optional examples

## Quick Start

```bash
git clone https://github.com/rahilsh/movie-ticket-booking-application.git
cd movie-ticket-booking-application
docker compose up --build -d
docker compose logs -f app
```

The API is available at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui/index.html`, and OpenAPI JSON is at `http://localhost:8080/v3/api-docs`.

Stop the services with:

```bash
docker compose down
```

To also delete local database data:

```bash
docker compose down -v
```

The credentials in `docker-compose.yml` are development-only.

## Local Maven Run

Start only PostgreSQL:

```bash
docker compose up -d postgres
JWT_SECRET="$(openssl rand -base64 48)" mvn spring-boot:run
```

The default profile is `dev`. Production deployments must use `SPRING_PROFILES_ACTIVE=prod` and supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`.

## Admin Setup

Registration creates `ROLE_USER` accounts only. For local development, register an account and promote it directly in PostgreSQL:

```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';
```

Use an auditable identity-management or provisioning process outside local development.

## API Overview

| Method and path | Access |
|---|---|
| `POST /api/auth/register` | Public |
| `POST /api/auth/login` | Public |
| `GET /api/theatres/**` | Public |
| `GET /api/screens/**` | Public |
| `GET /api/shows/**` | Public |
| `POST`, `PUT`, `DELETE /api/theatres/**` | Admin |
| `POST /api/theatres/{theatreId}/screens` | Admin |
| `POST /api/screens/{screenId}/shows` | Admin |
| `GET /api/screens/{screenId}/shows` | Public |
| `POST /api/bookings` | User |
| `GET /api/bookings/my` | User |
| `GET`, `DELETE /api/bookings/{id}` | Owner; reads also allow admin |
| `POST /api/payments` | Booking owner |
| `GET /api/payments/{id}` | Owner or admin |
| `GET /api/payments/booking/{bookingId}` | Owner or admin |
| `POST /api/payments/confirm/{transactionId}` | Admin, simulated callback |
| `POST /api/payments/fail/{transactionId}` | Admin, simulated callback |

Consult Swagger UI for request and response schemas.

## Configuration

| Environment variable | Required | Purpose |
|---|---|---|
| `DB_URL` | Production | PostgreSQL JDBC URL |
| `DB_USERNAME` | Production | Database user |
| `DB_PASSWORD` | Production | Database password |
| `JWT_SECRET` | Yes outside development | HMAC signing key, at least 256 bits |
| `JWT_EXPIRATION_MS` | No | Token lifetime; defaults to 24 hours |
| `SEAT_HOLD_DURATION` | No | ISO-8601 duration; defaults to `PT10M` |
| `BOOKING_EXPIRY_SCAN_DELAY_MS` | No | Expiry scan interval |
| `BOOKING_EXPIRY_BATCH_SIZE` | No | Maximum holds processed per scan |

Never use the Compose JWT key outside local development.

## Testing

Fast unit tests do not require Docker:

```bash
mvn clean test
```

The complete verification requires Docker. It starts PostgreSQL with Testcontainers, starts the API on a random port, calls real HTTP endpoints, and enforces instruction and branch coverage above 85%:

```bash
mvn clean verify
```

Reports are generated under `target/surefire-reports`, `target/failsafe-reports`, and `target/site/jacoco`.

## CI and Containers

GitHub Actions runs complete Maven verification on pushes and pull requests. On `main`, it also validates that the Docker image builds. The workflow does not publish an image registry package.

## Community

- [Contributing](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security Policy](SECURITY.md)

## License

Licensed under the [MIT License](LICENSE).
