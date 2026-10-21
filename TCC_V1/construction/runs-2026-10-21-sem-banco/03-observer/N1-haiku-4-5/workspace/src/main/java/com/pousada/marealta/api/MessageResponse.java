package com.pousada.marealta.api;

import com.pousada.marealta.model.Message;

public class MessageResponse {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public MessageResponse(Message message) {
        this.id = message.id;
        this.channel = message.channel;
        this.to = message.to;
        this.subject = message.subject;
        this.body = message.body;
    }
}
