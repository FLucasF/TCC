-- SENT for what went out, BLOCKED for what was not sent (for example, a malformed address)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
