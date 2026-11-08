package com.pousadamarealta.api.dto;

import com.pousadamarealta.domain.model.Message;

public record MessageResponse(
    Long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
            m.getId(),
            m.getChannel(),
            m.getTo(),
            m.getSubject(),
            m.getBody()
        );
    }
}
