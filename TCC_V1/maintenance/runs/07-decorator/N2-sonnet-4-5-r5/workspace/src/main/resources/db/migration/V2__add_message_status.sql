-- add status to messages so the front desk knows which were blocked
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
