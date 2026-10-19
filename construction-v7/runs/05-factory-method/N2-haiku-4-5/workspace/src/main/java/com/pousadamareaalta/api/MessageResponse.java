package com.pousadamareaalta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamareaalta.model.Message;

public record MessageResponse(
    @JsonProperty("id") long id,
    @JsonProperty("channel") String channel,
    @JsonProperty("to") String to,
    @JsonProperty("subject") String subject,
    @JsonProperty("body") String body
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.id(),
            message.channel(),
            message.to(),
            message.subject(),
            message.body()
        );
    }
}
