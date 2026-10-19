package com.pousada.marealta.api.converter;

import com.pousada.marealta.api.dto.MessageDto;
import com.pousada.marealta.model.Message;

public class MessageConverter {
    public static MessageDto toDto(Message message) {
        return new MessageDto(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            message.getStatus()
        );
    }
}
