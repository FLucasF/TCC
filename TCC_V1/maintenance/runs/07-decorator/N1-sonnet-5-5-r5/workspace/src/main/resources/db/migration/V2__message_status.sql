-- a message is either SENT or BLOCKED (not sent, for the front desk to follow up); the ones from before were sent
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
