-- Adds the children count to reservations. Children don't pay but occupy room capacity.
ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0);
