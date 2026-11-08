package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Message;

public record MessageResponse(
    @JsonProperty("id") Long id,
    @JsonProperty("channel") String channel,
    @JsonProperty("to") String to,
    @JsonProperty("subject") String subject,
    @JsonProperty("body") String body
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
            m.getId(),
            m.getChannel(),
            m.getTo(),
            m.getSubject(),
            m.getBody()
        );
    }
}
