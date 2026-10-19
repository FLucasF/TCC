package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;

public record MessageResponse(
    @JsonProperty("id") Long id,
    @JsonProperty("channel") MessageChannel channel,
    @JsonProperty("to") String to,
    @JsonProperty("subject") String subject,
    @JsonProperty("body") String body
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
