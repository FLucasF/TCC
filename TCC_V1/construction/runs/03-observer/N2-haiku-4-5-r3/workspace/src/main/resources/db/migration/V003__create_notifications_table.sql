CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES reservations(id),
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    body TEXT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_reservation ON notifications(reservation_id);
