package com.pousadamaralta.domain.model;

public class Message {
    private final Long id;
    private final Channel channel;
    private final String to;
    private final String subject;
    private final String body;

    public Message(Long id, Channel channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public Channel getChannel() {
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
