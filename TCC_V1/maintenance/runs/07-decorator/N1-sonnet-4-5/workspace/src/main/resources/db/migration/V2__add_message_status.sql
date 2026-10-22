-- Add status to track whether each message was sent or blocked
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
