package com.pousada.marealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.marealta.domain.Message;

public record MessageResponse(
    Long id,
    String channel,
    @JsonProperty("to") String to,
    String subject,
    String body,
    String status
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            message.getStatus().name()
        );
    }
}
