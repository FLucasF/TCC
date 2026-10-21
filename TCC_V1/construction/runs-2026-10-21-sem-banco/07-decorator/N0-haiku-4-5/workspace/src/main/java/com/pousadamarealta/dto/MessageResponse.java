package com.pousadamarealta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.Message;

public class MessageResponse {
    @JsonProperty("id")
    public long id;

    @JsonProperty("channel")
    public String channel;

    @JsonProperty("to")
    public String to;

    @JsonProperty("subject")
    public String subject;

    @JsonProperty("body")
    public String body;

    @JsonProperty("status")
    public String status;

    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        response.id = message.getId();
        response.channel = message.getChannel().toString();
        response.to = message.getTo();
        response.subject = message.getSubject();
        response.body = message.getBody();
        response.status = message.getStatus().toString();
        return response;
    }
}
