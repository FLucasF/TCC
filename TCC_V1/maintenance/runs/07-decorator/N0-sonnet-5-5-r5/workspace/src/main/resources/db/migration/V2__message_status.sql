-- BLOCKED messages were never sent (the front desk calls the guest instead).
ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT';
-- the address the guest gave us (recipient may be the test mailbox), used by the daily limit
ALTER TABLE messages ADD COLUMN guest_address text;
UPDATE messages SET guest_address = recipient;
-- archive copies are not messages "to the guest"
ALTER TABLE messages ADD COLUMN archive_copy boolean NOT NULL DEFAULT false;
-- when the message was handled, used by the daily limit
ALTER TABLE messages ADD COLUMN sent_at timestamptz NOT NULL DEFAULT now();
