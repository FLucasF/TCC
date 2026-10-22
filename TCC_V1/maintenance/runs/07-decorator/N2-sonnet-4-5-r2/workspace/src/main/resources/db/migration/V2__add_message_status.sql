-- Every message now has a status: SENT or BLOCKED
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
