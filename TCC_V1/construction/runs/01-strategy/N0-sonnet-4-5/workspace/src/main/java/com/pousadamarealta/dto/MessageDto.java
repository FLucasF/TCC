package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Message;

public record MessageDto(
        @JsonProperty("id") Long id,
        @JsonProperty("channel") String channel,
        @JsonProperty("to") String to,
        @JsonProperty("subject") String subject,
        @JsonProperty("body") String body
) {
    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                message.getChannel(),
                message.getTo(),
                message.getSubject(),
                message.getBody()
        );
    }
}
