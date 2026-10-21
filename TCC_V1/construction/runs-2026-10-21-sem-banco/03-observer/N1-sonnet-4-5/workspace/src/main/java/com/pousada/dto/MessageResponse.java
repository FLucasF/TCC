package com.pousada.dto;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;

public class MessageResponse {
    private long id;
    private MessageChannel channel;
    private String to;
    private String subject;
    private String body;

    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        response.id = message.getId();
        response.channel = message.getChannel();
        response.to = message.getTo();
        response.subject = message.getSubject();
        response.body = message.getBody();
        return response;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public void setChannel(MessageChannel channel) {
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
