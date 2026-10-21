package com.pousada.dto;

import com.pousada.model.Message;

public class MessageDTO {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public static MessageDTO from(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.id = message.getId();
        dto.channel = message.getChannel();
        dto.to = message.getTo();
        dto.subject = message.getSubject();
        dto.body = message.getBody();
        return dto;
    }
}
