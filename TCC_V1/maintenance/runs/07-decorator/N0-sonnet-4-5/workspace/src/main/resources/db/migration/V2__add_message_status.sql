-- Add status to messages so the front desk can see blocked messages
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
