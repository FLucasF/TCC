package com.pousada.api.dto;

import com.pousada.domain.Message;

public class MessageResponse {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public static MessageResponse fromDomain(Message message) {
        MessageResponse response = new MessageResponse();
        response.id = message.getId();
        response.channel = message.getChannelString();
        response.to = message.getTo();
        response.subject = message.getSubject();
        response.body = message.getBody();
        return response;
    }
}
