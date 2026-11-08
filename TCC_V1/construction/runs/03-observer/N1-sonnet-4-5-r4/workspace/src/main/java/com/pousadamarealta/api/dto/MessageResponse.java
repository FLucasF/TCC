package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.model.Message;

public class MessageResponse {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("to")
    private String to;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("body")
    private String body;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.channel = message.getChannel();
        this.to = message.getTo();
        this.subject = message.getSubject();
        this.body = message.getBody();
    }

    public Long getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
