CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    room_number INTEGER NOT NULL REFERENCES rooms(number),
    guest_name VARCHAR(255) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_cpf VARCHAR(14) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guest_count INTEGER NOT NULL,
    rate_plan VARCHAR(20) NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_paid_cents BIGINT NOT NULL DEFAULT 0,
    refund_cents BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_room ON reservations(room_number);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_dates ON reservations(check_in, check_out);
