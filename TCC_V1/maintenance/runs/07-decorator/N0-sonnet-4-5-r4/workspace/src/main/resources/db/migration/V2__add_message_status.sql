-- Add status field to track sent vs blocked messages
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
