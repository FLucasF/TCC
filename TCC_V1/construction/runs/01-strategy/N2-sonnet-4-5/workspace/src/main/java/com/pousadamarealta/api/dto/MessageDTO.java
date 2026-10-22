package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.Message;

public record MessageDTO(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageDTO from(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
