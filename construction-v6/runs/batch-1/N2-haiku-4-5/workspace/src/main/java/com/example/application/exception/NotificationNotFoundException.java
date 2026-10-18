package com.example.application.exception;

public class NotificationNotFoundException extends ApplicationException {
    public NotificationNotFoundException() {
        super("NOTIFICATION_NOT_FOUND", 404);
    }
}
