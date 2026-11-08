package com.pousada.api.dto;

import com.pousada.infra.MessageRepository;

public class MessageResponse {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public static MessageResponse from(MessageRepository.Message message) {
        MessageResponse dto = new MessageResponse();
        dto.id = message.id();
        dto.channel = message.channel();
        dto.to = message.to();
        dto.subject = message.subject();
        dto.body = message.body();
        return dto;
    }
}
