package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.model.Message;

public record MessageResponse(
    Long id,
    String channel,
    @JsonProperty("to") String to,
    String subject,
    String body
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
