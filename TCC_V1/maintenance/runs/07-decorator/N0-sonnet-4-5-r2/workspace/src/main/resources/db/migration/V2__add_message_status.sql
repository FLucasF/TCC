-- Add status column to messages table
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
