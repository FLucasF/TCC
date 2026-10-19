package com.pousadameraalta.api.dto;

import com.pousadameraalta.model.Message;

public class MessageResponse {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.channel = message.getChannel();
        this.to = message.getTo();
        this.subject = message.getSubject();
        this.body = message.getBody();
    }
}
