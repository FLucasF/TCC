-- Add status column to messages table to track SENT vs BLOCKED messages
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
