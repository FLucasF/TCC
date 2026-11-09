-- children up to 12 years old: they take a place in the room but do not pay
ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0);
