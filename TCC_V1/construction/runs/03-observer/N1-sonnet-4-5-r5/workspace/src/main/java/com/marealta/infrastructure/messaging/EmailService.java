package com.marealta.infrastructure.messaging;

public class EmailService {
    private final MessageStore messageStore;

    public EmailService(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    public void send(String to, String subject, String body) {
        messageStore.save("EMAIL", to, subject, body);
    }
}
