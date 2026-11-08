package com.pousada.marealta.domain;

public class Message {
    private long id;
    private Channel channel;
    private String to;
    private String subject;
    private String body;

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

    public enum Channel {
        EMAIL
    }
}
