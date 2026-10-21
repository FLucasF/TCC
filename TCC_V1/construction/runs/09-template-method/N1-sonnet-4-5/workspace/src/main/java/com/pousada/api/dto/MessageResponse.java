package com.pousada.api.dto;

import com.pousada.domain.Message;

public class MessageResponse {
    private long id;
    private String channel;
    private String to;
    private String subject;
    private String body;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.channel = message.getChannel().name();
        this.to = message.getTo();
        this.subject = message.getSubject();
        this.body = message.getBody();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
