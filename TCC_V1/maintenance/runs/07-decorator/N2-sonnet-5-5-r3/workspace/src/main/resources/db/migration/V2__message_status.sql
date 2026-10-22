-- SENT for what went out, BLOCKED for what was held back (the front desk calls the guest)
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
