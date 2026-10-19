package com.pousadamarealta.web;

import com.pousadamarealta.messaging.Message;

public class MessageResponse {
    private long id;
    private String channel;
    private String to;
    private String subject;
    private String body;
    private String status;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.channel = message.getChannel().name();
        this.to = message.getTo();
        this.subject = message.getSubject();
        this.body = message.getBody();
        this.status = message.getStatus().name();
    }

    public long getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }
}
