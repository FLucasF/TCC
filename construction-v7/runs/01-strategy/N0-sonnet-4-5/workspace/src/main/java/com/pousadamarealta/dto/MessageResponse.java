package com.pousadamarealta.dto;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;

public class MessageResponse {
    private long id;
    private MessageChannel channel;
    private String to;
    private String subject;
    private String body;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.channel = message.getChannel();
        this.to = message.getTo();
        this.subject = message.getSubject();
        this.body = message.getBody();
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
