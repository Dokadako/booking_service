# Roadmap

The skeleton (auth, rooms, infrastructure) is done. The booking logic below is the core of the project
and is meant to be written by hand: it is what you will be asked about in interviews.

After each task: run `test-writer`, then `code-reviewer`, and fix every Blocker and Major before committing.

## 1. Bookings: create and list ✅
- Migration `V3__create_bookings.sql`: `id`, `room_id` (FK), `user_id` (FK), `starts_at`, `ends_at` (TIMESTAMPTZ),
  `status` (`CONFIRMED` / `CANCELLED`), `created_at`, `version` (for optimistic locking).
- `POST /api/bookings` `{roomId, startsAt, endsAt}`, `GET /api/bookings/my` (paged).
- Validation: `startsAt` before `endsAt`, in the future, duration from 15 minutes to 8 hours, aligned to 15 minutes.
- Learn: `@ManyToOne(fetch = LAZY)`, why `Instant` and not `LocalDateTime`, injecting `Clock` for tests.

## 2. No double booking (the hard part)
Two bookings of the same room must not overlap: `a.start < b.end AND b.start < a.end`.
- First do it with a check in the service and write a test that sends 10 parallel requests for the same slot
  (`ExecutorService` + `CountDownLatch`). The test will fail: exactly one should succeed, but several will. Understand why.
- Then fix it properly. Compare and pick one, and write down why in the README:
  - PostgreSQL exclusion constraint: `EXCLUDE USING gist (room_id WITH =, tstzrange(starts_at, ends_at) WITH &&) WHERE (status = 'CONFIRMED')`
    (needs `CREATE EXTENSION btree_gist`), translate the violation into `409 Conflict`;
  - pessimistic lock on the room row (`@Lock(PESSIMISTIC_WRITE)`).
- Learn: transaction isolation levels, race conditions, why a check-then-insert is not safe.

## 3. Cancel and ownership
- `DELETE /api/bookings/{id}`: only the owner or an admin; cannot cancel a booking that already started.
- Deleting a room that has bookings currently fails on the foreign key with a 500. Decide: forbid it with `409`,
  or replace hard delete with deactivating the room (keeps booking history), and test it.
- Test that user A cannot see or cancel user B's booking (IDOR), expecting `404`, not `403`.

## 4. Availability
- `GET /api/rooms/{id}/availability?date=2026-11-01`: free slots of the day.
- `GET /api/rooms/available?from=...&to=...&minCapacity=...`: rooms free for the whole interval, in one SQL query.
- Run `performance-optimizer`: check for N+1, add indexes on `(room_id, starts_at)`.

## 5. Production touches
- Rate limiting on `/api/auth/login`.
- Scheduled job that marks finished bookings, with a test using a fixed `Clock`.
- Notifications on booking via an outbox table and a scheduled publisher (later: Kafka).
- Deploy (Render / Fly.io) and put the live Swagger link in the README.
