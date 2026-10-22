-- a message is either sent or blocked (mistyped address); the ones already kept were sent
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
