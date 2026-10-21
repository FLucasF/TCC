package com.pousadamarealta.dto;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;

public record MessageResponse(
    long id,
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
