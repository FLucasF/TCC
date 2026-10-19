package com.pousada.api.dto;

import com.pousada.domain.model.Message;
import com.pousada.domain.model.MessageChannel;

public record MessageResponse(
    Long id,
    MessageChannel channel,
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
