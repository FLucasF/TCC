-- Add status column to messages table (SENT or BLOCKED)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
