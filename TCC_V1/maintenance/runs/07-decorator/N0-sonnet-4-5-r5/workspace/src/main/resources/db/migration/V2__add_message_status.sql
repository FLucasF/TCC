-- Add status column to messages table to track sent vs blocked messages
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
