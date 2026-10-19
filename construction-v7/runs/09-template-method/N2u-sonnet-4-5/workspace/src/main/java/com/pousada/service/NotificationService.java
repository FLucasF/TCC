package com.pousada.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class NotificationService {
    private final List<Message> sentMessages;
    private final AtomicLong idGenerator;

    public NotificationService() {
        this.sentMessages = new ArrayList<>();
        this.idGenerator = new AtomicLong(1);
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message(
                idGenerator.getAndIncrement(),
                "EMAIL",
                to,
                subject,
                body
        );
        sentMessages.add(message);
    }

    public List<Message> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }

    public static class Message {
        private final long id;
        private final String channel;
        private final String to;
        private final String subject;
        private final String body;

        public Message(long id, String channel, String to, String subject, String body) {
            this.id = id;
            this.channel = channel;
            this.to = to;
            this.subject = subject;
            this.body = body;
        }

        public long getId() {
            return id;
        }

        public String getChannel() {
            return channel;
        }

        public String getTo() {
            return to;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }
    }
}
