-- messages that were kept but not sent (e.g. invalid address) are BLOCKED; the ones already sent stay SENT
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
