package com.pousadamarealta.api;

public class MessageResponse {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;
    public String status;

    public MessageResponse(long id, String channel, String to, String subject, String body, String status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }
}
