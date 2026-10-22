-- Add status to messages (SENT or BLOCKED)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
