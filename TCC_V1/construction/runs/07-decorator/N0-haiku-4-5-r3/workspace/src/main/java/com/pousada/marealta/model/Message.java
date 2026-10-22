package com.pousada.marealta.model;

public class Message {
    public int id;
    public String channel;
    public String to;
    public String subject;
    public String body;
    public String status;

    public Message() {}

    public Message(int id, String channel, String to, String subject, String body, String status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }
}
