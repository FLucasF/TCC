package com.pousadamaralta.api.dto;

import com.pousadamaralta.domain.model.Message;

public record MessageResponse(
    Long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
