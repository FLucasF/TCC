package com.pousadamarealta.controller.dto;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;

public class MessageResponse {
    private Long id;
    private MessageChannel channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        response.id = message.getId();
        response.channel = message.getChannel();
        response.to = message.getTo();
        response.subject = message.getSubject();
        response.body = message.getBody();
        response.status = message.getStatus();
        return response;
    }

    public Long getId() {
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

    public MessageStatus getStatus() {
        return status;
    }
}
