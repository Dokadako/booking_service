# Booking Service

REST API for booking meeting rooms: users register, browse rooms and book time slots;
administrators manage rooms. The focus is correctness under concurrent requests
(no double booking), security and test coverage.

> Status: in progress. Auth and room management are done; bookings are being built (see [docs/TASKS.md](docs/TASKS.md)).

## Stack
Java 21 · Spring Boot 4 · Spring Security (JWT, OAuth2 resource server) · Spring Data JPA / Hibernate ·
PostgreSQL · Flyway · JUnit 5, Mockito, Testcontainers · Docker Compose · GitHub Actions · OpenAPI

## Architecture
```
controller  →  service (@Transactional, business rules)  →  repository (Spring Data JPA)  →  PostgreSQL
   DTOs (records)        entities never leave the service layer         schema via Flyway migrations
```
Code is organised by feature (`auth`, `user`, `room`), not by layer. Errors are returned as
RFC 9457 problem details from a single `GlobalExceptionHandler`.

## Run locally
```bash
cp .env.example .env        # then set DB_PASSWORD, JWT_SECRET (openssl rand -base64 32), admin credentials
docker compose up --build
```
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Tests
```bash
./mvnw verify
```
Integration tests start a real PostgreSQL with Testcontainers, so Docker must be running.

## API
| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register` | public |
| POST | `/api/auth/login` | public |
| GET | `/api/rooms`, `/api/rooms/{id}` | authenticated |
| POST, PUT, DELETE | `/api/rooms`, `/api/rooms/{id}` | ADMIN |

## Security notes
- Passwords hashed with BCrypt; login returns the same error for unknown email and wrong password.
- JWT signed with HS256; the key comes from `JWT_SECRET` and the app refuses to start with a key shorter than 256 bits.
- No credentials in code: the first admin is created from `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
