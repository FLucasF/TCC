-- BLOCKED messages were not sent (the front desk calls the guest). original_recipient is the guest's address when
-- the message went somewhere else (test mode); sent_at lets us count the messages a guest got on a day.
ALTER TABLE messages
    ADD COLUMN status             text NOT NULL DEFAULT 'SENT',
    ADD COLUMN original_recipient text,
    ADD COLUMN sent_at            timestamptz NOT NULL DEFAULT now();
