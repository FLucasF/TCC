package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Message;

public record MessageResponse(
    @JsonProperty("id") long id,
    @JsonProperty("channel") String channel,
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
