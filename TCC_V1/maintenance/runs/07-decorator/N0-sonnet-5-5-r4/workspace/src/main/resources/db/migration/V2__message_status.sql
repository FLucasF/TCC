-- whether the message went out (SENT) or a sending rule stopped it (BLOCKED); the old ones all went out
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
