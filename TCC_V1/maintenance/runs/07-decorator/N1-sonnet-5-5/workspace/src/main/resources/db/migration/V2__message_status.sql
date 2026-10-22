-- a message is either sent or blocked (not sent, for the front desk to follow up)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
