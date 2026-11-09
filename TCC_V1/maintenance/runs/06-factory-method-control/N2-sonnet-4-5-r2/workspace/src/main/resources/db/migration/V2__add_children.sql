-- Children (up to 12 years) staying in the room. They don't pay but take room capacity.
ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0);
