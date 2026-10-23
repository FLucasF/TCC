package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Message;

public record MessageResponse(
    long id,
    String channel,
    String to,
    String subject,
    String body,
    String status
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            message.getStatus().name()
        );
    }
}
