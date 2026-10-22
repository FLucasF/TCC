package com.pousadamarealta;

public class Message {
    public final int id;
    public final String channel;
    public final String to;
    public final String subject;
    public final String body;

    public Message(int id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
}
