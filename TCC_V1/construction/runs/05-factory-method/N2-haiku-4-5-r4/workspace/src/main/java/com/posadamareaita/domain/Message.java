package com.posadamareaita.domain;

public class Message {
    private long id;
    private String channel;
    private String to;
    private String subject;
    private String body;

    public Message(String channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getChannel() { return channel; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
