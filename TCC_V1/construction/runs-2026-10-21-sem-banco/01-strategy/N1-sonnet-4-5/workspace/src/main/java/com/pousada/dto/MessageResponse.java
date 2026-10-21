package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.model.Message;
import com.pousada.model.MessageChannel;

public class MessageResponse {
    @JsonProperty("id")
    public long id;

    @JsonProperty("channel")
    public MessageChannel channel;

    @JsonProperty("to")
    public String to;

    @JsonProperty("subject")
    public String subject;

    @JsonProperty("body")
    public String body;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.channel = message.getChannel();
        this.to = message.getTo();
        this.subject = message.getSubject();
        this.body = message.getBody();
    }
}
