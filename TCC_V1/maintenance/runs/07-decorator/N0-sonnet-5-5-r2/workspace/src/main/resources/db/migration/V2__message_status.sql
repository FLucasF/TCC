-- a message is SENT, or BLOCKED when a rule held it back (the front desk then calls the guest)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
