-- children (up to 12 years old) staying with the adults: they take room in the bedroom but do not pay
ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0);
