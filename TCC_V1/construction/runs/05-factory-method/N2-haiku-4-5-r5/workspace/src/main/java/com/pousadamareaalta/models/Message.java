package com.pousadamareaalta.models;

public class Message {
    public Long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public Message(String channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public Message() {}
}
