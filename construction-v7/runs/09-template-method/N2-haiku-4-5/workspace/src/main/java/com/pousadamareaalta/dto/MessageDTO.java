package com.pousadamareaalta.dto;

import com.pousadamareaalta.model.Message;

public class MessageDTO {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public static MessageDTO fromMessage(Message msg) {
        MessageDTO dto = new MessageDTO();
        dto.id = msg.getId();
        dto.channel = msg.getChannel();
        dto.to = msg.getTo();
        dto.subject = msg.getSubject();
        dto.body = msg.getBody();
        return dto;
    }
}
