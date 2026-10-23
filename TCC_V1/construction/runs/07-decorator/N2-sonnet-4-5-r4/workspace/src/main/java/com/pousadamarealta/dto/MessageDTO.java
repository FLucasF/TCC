package com.pousadamarealta.dto;

import com.pousadamarealta.domain.Message;

public record MessageDTO(
    long id,
    String channel,
    String to,
    String subject,
    String body,
    String status
) {
    public static MessageDTO from(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            message.getStatus().name()
        );
    }
}
