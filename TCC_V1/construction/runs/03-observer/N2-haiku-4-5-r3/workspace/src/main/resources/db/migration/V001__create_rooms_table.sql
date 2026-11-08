CREATE TABLE rooms (
    number INTEGER PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    capacity INTEGER NOT NULL,
    nightly_rate_cents BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
