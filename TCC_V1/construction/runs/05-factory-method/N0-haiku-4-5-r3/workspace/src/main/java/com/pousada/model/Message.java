package com.pousada.model;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private String channel;
    private String to;
    private String subject;
    private String body;
    private LocalDateTime createdAt;

    public Message(int id, String channel, String to, String subject, String body, LocalDateTime createdAt) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt;
    }

    public int getId() {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
