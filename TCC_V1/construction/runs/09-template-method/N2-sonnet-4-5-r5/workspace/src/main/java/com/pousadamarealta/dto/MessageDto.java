package com.pousadamarealta.dto;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;

public record MessageDto(
    Long id,
    MessageChannel channel,
    String to,
    String subject,
    String body
) {
    public static MessageDto from(Message m) {
        return new MessageDto(
            m.getId(),
            m.getChannel(),
            m.getTo(),
            m.getSubject(),
            m.getBody()
        );
    }
}
