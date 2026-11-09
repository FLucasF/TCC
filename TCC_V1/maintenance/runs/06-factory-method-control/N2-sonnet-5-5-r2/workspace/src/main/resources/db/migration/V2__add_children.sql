-- children (up to 12 years old) take a place in the room but pay no nightly rate; `guests` counts only the adults
ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0);
