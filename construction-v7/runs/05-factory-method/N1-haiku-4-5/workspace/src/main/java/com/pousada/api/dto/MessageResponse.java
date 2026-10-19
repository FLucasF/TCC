package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.Message;

public class MessageResponse {
    @JsonProperty("id")
    public Long id;

    @JsonProperty("channel")
    public String channel;

    @JsonProperty("to")
    public String to;

    @JsonProperty("subject")
    public String subject;

    @JsonProperty("body")
    public String body;

    public static MessageResponse fromMessage(Message msg) {
        MessageResponse dto = new MessageResponse();
        dto.id = msg.getId();
        dto.channel = msg.getChannel().toString();
        dto.to = msg.getTo();
        dto.subject = msg.getSubject();
        dto.body = msg.getBody();
        return dto;
    }
}
