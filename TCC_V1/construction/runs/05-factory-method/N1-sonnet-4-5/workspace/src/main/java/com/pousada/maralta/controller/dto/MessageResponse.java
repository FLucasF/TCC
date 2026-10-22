package com.pousada.maralta.controller.dto;

import com.pousada.maralta.domain.Message;

public record MessageResponse(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
