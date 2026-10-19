package com.pousadamareal.domain;

public class Message {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public enum Channel {
        EMAIL
    }

    public Message(long id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
}
