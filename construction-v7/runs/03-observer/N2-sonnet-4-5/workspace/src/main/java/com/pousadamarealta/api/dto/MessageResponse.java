package com.pousadamarealta.api.dto;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;

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

    public MessageChannel getChannel() {
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
