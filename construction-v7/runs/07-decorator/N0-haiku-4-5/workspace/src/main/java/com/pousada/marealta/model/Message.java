package com.pousada.marealta.model;

public class Message {
    private final long id;
    private final String channel;
    private final String to;
    private final String subject;
    private final String body;
    private final String status;

    public Message(long id, String channel, String to, String subject, String body, String status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public long getId() { return id; }
    public String getChannel() { return channel; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getStatus() { return status; }
}
