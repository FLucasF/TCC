package com.pousada.domain;

import java.time.LocalDateTime;

public class Message {
    public final int id;
    public final String channel;
    public final String to;
    public final String subject;
    public final String body;
    public final LocalDateTime sentAt;

    public Message(int id, String channel, String to, String subject, String body, LocalDateTime sentAt) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.sentAt = sentAt;
    }

    public Message(String channel, String to, String subject, String body) {
        this(0, channel, to, subject, body, LocalDateTime.now());
    }
}
