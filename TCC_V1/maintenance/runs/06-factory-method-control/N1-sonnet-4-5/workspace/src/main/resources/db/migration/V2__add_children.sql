-- Adds the number of children (under 12) in each reservation.
ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0);
