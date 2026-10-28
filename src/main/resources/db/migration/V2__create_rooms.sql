CREATE TABLE rooms
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    capacity    INT          NOT NULL CHECK (capacity > 0),
    description VARCHAR(500)
);

CREATE UNIQUE INDEX ux_rooms_name_lower ON rooms (LOWER(name));
