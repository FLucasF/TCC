package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.models.Message;
import com.pousada.models.MessageStatus;

public record MessageResponse(
        @JsonProperty("id")
        long id,
        @JsonProperty("channel")
        String channel,
        @JsonProperty("to")
        String to,
        @JsonProperty("subject")
        String subject,
        @JsonProperty("body")
        String body,
        @JsonProperty("status")
        MessageStatus status
) {
    public static MessageResponse fromMessage(Message msg) {
        return new MessageResponse(
                msg.getId(),
                msg.getChannel(),
                msg.getTo(),
                msg.getSubject(),
                msg.getBody(),
                msg.getStatus()
        );
    }
}
