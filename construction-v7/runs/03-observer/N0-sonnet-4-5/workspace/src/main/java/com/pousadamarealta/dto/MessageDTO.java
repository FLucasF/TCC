package com.pousadamarealta.dto;

import com.pousadamarealta.notification.NotificationMessage;

public record MessageDTO(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageDTO from(NotificationMessage message) {
        return new MessageDTO(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
