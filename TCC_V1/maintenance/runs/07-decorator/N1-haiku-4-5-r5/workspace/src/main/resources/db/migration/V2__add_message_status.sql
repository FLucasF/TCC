-- Track whether messages were sent or blocked (invalid email, test mode, etc.)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
