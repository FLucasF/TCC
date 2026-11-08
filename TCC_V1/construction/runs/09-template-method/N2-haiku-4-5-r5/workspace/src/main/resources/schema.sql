CREATE TABLE IF NOT EXISTS rooms (
    number INTEGER PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    capacity INTEGER NOT NULL,
    nightly_rate NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    id SERIAL PRIMARY KEY,
    room_number INTEGER NOT NULL REFERENCES rooms(number),
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_cpf VARCHAR(14) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guests INTEGER NOT NULL,
    rate_plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    nights INTEGER NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    discount NUMERIC(10, 2) NOT NULL,
    total NUMERIC(10, 2) NOT NULL,
    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
    refund NUMERIC(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    channel VARCHAR(20) NOT NULL,
    "to" VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reservations_room_dates ON reservations(room_number, check_in, check_out)
    WHERE status NOT IN ('CANCELLED');

CREATE INDEX IF NOT EXISTS idx_reservations_check_in ON reservations(check_in)
    WHERE status NOT IN ('CANCELLED');
