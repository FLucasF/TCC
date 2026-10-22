-- whether the message went out (SENT) or was held back for the front desk to follow up (BLOCKED)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
