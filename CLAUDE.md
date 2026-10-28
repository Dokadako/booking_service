# Project conventions

Java 21, Spring Boot 4, PostgreSQL, Maven wrapper, Docker Compose.

- Layers: controller → service → repository. Controllers only map HTTP; business logic lives in services.
- DTOs are Java records; entities never leave the service layer.
- Constructor injection only.
- Schema changes only through new Flyway migrations; never edit an applied one.
- Money is `BigDecimal`; time is `Instant`/`Clock`-based and injectable.
- Secrets only from environment variables.
- Every feature ships with tests: unit (JUnit 5, Mockito, AssertJ) and integration (Testcontainers).

## Workflow
After implementing a feature, use the subagents in this order:
1. `test-writer`: add and run tests.
2. `code-reviewer`: fix every Blocker and Major it reports.
3. `performance-optimizer`: for features that touch the database or large data.
4. `security-auditor`: before deploys and before making the repo public.
