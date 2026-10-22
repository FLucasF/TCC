-- Add status column to track if messages were sent or blocked
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
