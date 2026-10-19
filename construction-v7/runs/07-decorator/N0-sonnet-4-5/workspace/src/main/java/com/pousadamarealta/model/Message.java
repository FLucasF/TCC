package com.pousadamarealta.model;

public class Message {
    private final Long id;
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;
    private final MessageStatus status;

    public Message(Long id, MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public MessageChannel getChannel() {
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

    public MessageStatus getStatus() {
        return status;
    }
}
