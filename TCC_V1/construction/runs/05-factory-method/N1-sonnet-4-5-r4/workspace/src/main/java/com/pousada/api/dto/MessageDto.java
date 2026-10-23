package com.pousada.api.dto;

import com.pousada.domain.Message;

public record MessageDto(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageDto from(Message message) {
        return new MessageDto(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
