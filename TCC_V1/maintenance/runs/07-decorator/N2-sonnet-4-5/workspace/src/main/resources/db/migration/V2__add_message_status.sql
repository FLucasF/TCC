-- Add status field to track whether a message was sent or blocked
ALTER TABLE messages ADD COLUMN status TEXT NOT NULL DEFAULT 'SENT';
