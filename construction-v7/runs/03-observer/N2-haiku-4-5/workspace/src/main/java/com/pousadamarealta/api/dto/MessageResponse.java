package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Message;

public record MessageResponse(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.id(),
            message.channel().name(),
            message.to(),
            message.subject(),
            message.body()
        );
    }
}
