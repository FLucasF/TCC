package com.pousada.dto;

import com.pousada.models.Message;

public class MessageDTO {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public MessageDTO(Message message) {
        this.id = message.id;
        this.channel = message.channel;
        this.to = message.to;
        this.subject = message.subject;
        this.body = message.body;
    }
}
