package com.pousadamarealta.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Message;

public record MessageResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("channel") String channel,
        @JsonProperty("to") String to,
        @JsonProperty("subject") String subject,
        @JsonProperty("body") String body,
        @JsonProperty("status") String status
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChannel().name(),
                message.getTo(),
                message.getSubject(),
                message.getBody(),
                message.getStatus().name()
        );
    }
}
