package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Message;

public record MessageResponse(
    @JsonProperty("id") long id,
    @JsonProperty("channel") String channel,
    @JsonProperty("to") String to,
    @JsonProperty("subject") String subject,
    @JsonProperty("body") String body
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(m.id(), m.channel(), m.to(), m.subject(), m.body());
    }
}
