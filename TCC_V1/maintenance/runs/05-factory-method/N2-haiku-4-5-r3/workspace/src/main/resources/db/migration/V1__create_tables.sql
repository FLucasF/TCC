-- The inn's rooms. They rarely change, so they come with the schema.
CREATE TABLE rooms (
    number       integer PRIMARY KEY,
    type         text NOT NULL,
    capacity     integer NOT NULL CHECK (capacity > 0),
    nightly_rate numeric(10, 2) NOT NULL CHECK (nightly_rate > 0)
);

INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES
    (101, 'STANDARD', 2, 249.90),
    (102, 'STANDARD', 2, 249.90),
    (201, 'DELUXE', 3, 389.90),
    (202, 'DELUXE', 3, 389.90),
    (301, 'SUITE', 4, 579.90);

-- btree_gist lets one constraint compare the room number (equality) and the nights (overlap) together.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservations (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_number integer NOT NULL REFERENCES rooms (number),
    guest_name  text NOT NULL,
    guest_email text NOT NULL,
    guest_cpf   text NOT NULL,
    check_in    date NOT NULL,
    check_out   date NOT NULL,
    guests      integer NOT NULL CHECK (guests >= 1),
    rate_plan   text NOT NULL,
    nights      integer NOT NULL,
    subtotal    numeric(10, 2) NOT NULL,
    discount    numeric(10, 2) NOT NULL,
    total       numeric(10, 2) NOT NULL,
    status      text NOT NULL,
    amount_paid numeric(10, 2) NOT NULL,
    refund      numeric(10, 2),
    CHECK (check_out > check_in),
    -- a room is never booked twice for the same night, even when two requests arrive at the same time
    CONSTRAINT reservations_no_double_booking
        EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&)
        WHERE (status <> 'CANCELLED')
);

-- every message the inn sent, so the front desk can check what each guest received
CREATE TABLE messages (
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    channel   text NOT NULL,
    recipient text NOT NULL,
    subject   text NOT NULL,
    body      text NOT NULL
);
