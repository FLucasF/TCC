package com.posadamarealta.model;

public class Message {
    public int id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public Message(int id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
}
