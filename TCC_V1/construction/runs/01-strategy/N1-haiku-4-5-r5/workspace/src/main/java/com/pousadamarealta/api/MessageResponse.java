package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.models.Message;

public class MessageResponse {
    @JsonProperty("id")
    public final int id;

    @JsonProperty("channel")
    public final String channel;

    @JsonProperty("to")
    public final String to;

    @JsonProperty("subject")
    public final String subject;

    @JsonProperty("body")
    public final String body;

    public MessageResponse(Message message) {
        this.id = message.id;
        this.channel = message.channel;
        this.to = message.to;
        this.subject = message.subject;
        this.body = message.body;
    }
}
