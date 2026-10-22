package com.pousadamarealta.messaging;

public final class SentMessage {
    private final Long id;
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;
    private final MessageStatus status;

    public SentMessage(Long id, MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public Long id() { return id; }
    public MessageChannel channel() { return channel; }
    public String to() { return to; }
    public String subject() { return subject; }
    public String body() { return body; }
    public MessageStatus status() { return status; }
}
