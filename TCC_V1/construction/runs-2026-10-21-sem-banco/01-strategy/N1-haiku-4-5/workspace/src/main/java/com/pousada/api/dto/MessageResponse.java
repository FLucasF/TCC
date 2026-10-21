package com.pousada.api.dto;

import com.pousada.domain.Message;

public class MessageResponse {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public static MessageResponse fromMessage(Message msg) {
        MessageResponse response = new MessageResponse();
        response.id = msg.getId();
        response.channel = msg.getChannel();
        response.to = msg.getTo();
        response.subject = msg.getSubject();
        response.body = msg.getBody();
        return response;
    }
}
