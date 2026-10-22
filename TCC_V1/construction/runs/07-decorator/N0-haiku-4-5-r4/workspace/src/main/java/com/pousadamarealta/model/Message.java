package com.pousadamarealta.model;

public class Message {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;
    public String status;

    public Message(String channel, String to, String subject, String body, String status) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public Message() {}
}
