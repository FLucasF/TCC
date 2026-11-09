-- Add support for children in reservations
ALTER TABLE reservations
    ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0);
