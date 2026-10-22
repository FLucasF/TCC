-- a message is SENT, or BLOCKED when it was not sent (for instance, an invalid address)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
