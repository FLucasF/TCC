-- a message is either sent or blocked (for instance, when the address is not valid)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
