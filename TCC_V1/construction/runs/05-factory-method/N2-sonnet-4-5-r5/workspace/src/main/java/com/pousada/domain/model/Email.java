package com.pousada.domain.model;

public final class Email {
    private long id;
    private final String channel;
    private final String to;
    private final String subject;
    private final String body;

    public Email(String to, String subject, String body) {
        this.channel = "EMAIL";
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public Email(long id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public long id() { return id; }
    public String channel() { return channel; }
    public String to() { return to; }
    public String subject() { return subject; }
    public String body() { return body; }

    public void setId(long id) {
        this.id = id;
    }
}
