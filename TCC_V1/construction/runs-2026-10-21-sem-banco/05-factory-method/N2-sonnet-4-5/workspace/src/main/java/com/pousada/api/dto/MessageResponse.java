package com.pousada.api.dto;

import com.pousada.domain.Message;

public class MessageResponse {
    private long id;
    private String channel;
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
}
