-- add status to messages to distinguish sent from blocked ones
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
