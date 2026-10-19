package com.pousadamarealta.api.mapper;

import com.pousadamarealta.api.dto.MessageDTO;
import com.pousadamarealta.domain.Message;

public class MessageMapper {
    public static MessageDTO toDTO(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getChannel().toString(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
