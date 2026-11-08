CREATE TABLE IF NOT EXISTS rooms (
    number INTEGER PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    capacity INTEGER NOT NULL,
    nightly_rate DECIMAL(10, 2) NOT NULL
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
    rate_plan VARCHAR(50) NOT NULL,
    company_code VARCHAR(50),
    nights INTEGER NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    discount DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
    refund DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_guests CHECK (guests >= 1),
    CONSTRAINT check_nights CHECK (nights >= 1),
    CONSTRAINT check_dates CHECK (check_out > check_in)
);

CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reservations_room_dates ON reservations(room_number, check_in, check_out);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_messages_channel ON messages(channel);
