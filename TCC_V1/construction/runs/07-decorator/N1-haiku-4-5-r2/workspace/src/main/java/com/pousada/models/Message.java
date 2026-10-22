package com.pousada.models;

public class Message {
    public enum Channel {
        EMAIL
    }

    public enum Status {
        SENT, BLOCKED
    }

    private final long id;
    private final Channel channel;
    private final String to;
    private final String subject;
    private final String body;
    private final Status status;

    public Message(long id, Channel channel, String to, String subject, String body, Status status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public long getId() { return id; }
    public Channel getChannel() { return channel; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Status getStatus() { return status; }
}
