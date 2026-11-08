package com.marealta.pousada.dto;

import com.marealta.pousada.domain.model.Message;

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
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
