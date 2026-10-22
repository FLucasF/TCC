package com.pousadamarealta.controller.dto;

import com.pousadamarealta.messaging.SentMessage;

public record MessageDto(
    Long id,
    String channel,
    String to,
    String subject,
    String body,
    String status
) {
    public static MessageDto from(SentMessage message) {
        return new MessageDto(
            message.id(),
            message.channel().name(),
            message.to(),
            message.subject(),
            message.body(),
            message.status().name()
        );
    }
}
