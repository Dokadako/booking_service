CREATE TABLE bookings
(
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT      NOT NULL REFERENCES rooms (id),
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    status     VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_bookings_time_range CHECK (starts_at < ends_at)
);

-- "My bookings" filters by user and sorts by start time.
CREATE INDEX ix_bookings_user_id_starts_at ON bookings (user_id, starts_at);
-- Overlap checks look up bookings of one room around a time range.
CREATE INDEX ix_bookings_room_id_starts_at ON bookings (room_id, starts_at);
