package com.pousadamarealta.domain;

public class Message {
    private final long id;
    private final String channel;
    private final String to;
    private final String subject;
    private final String body;
    private final MessageStatus status;

    public Message(long id, String channel, String to, String subject, String body, MessageStatus status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public long id() {
        return id;
    }

    public String channel() {
        return channel;
    }

    public String to() {
        return to;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public MessageStatus status() {
        return status;
    }

    public Message withStatus(MessageStatus newStatus) {
        return new Message(id, channel, to, subject, body, newStatus);
    }

    public Message withModifications(String newTo, String newSubject, String newBody) {
        return new Message(id, channel, newTo, newSubject, newBody, status);
    }
}
