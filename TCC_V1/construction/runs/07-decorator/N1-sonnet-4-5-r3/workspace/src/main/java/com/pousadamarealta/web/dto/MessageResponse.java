package com.pousadamarealta.web.dto;

import com.pousadamarealta.domain.Message;

public record MessageResponse(
    long id,
    String channel,
    String to,
    String subject,
    String body,
    String status
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
            m.getId(),
            m.getChannel().name(),
            m.getTo(),
            m.getSubject(),
            m.getBody(),
            m.getStatus().name()
        );
    }
}
