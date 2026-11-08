package com.marealta.domain;

public class Message {
    public int id;
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
}
