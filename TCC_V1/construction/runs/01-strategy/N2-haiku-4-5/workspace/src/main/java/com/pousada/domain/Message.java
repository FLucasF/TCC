package com.pousada.domain;

public class Message {
    public enum Channel {
        EMAIL
    }

    private final long id;
    private final Channel channel;
    private final String to;
    private final String subject;
    private final String body;

    public Message(long id, Channel channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public long getId() { return id; }
    public Channel getChannel() { return channel; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
