package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Message;

public class MessageDto {
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

    public static MessageDto fromMessage(Message message) {
        MessageDto dto = new MessageDto();
        dto.id = message.getId();
        dto.channel = message.getChannel().toString();
        dto.to = message.getTo();
        dto.subject = message.getSubject();
        dto.body = message.getBody();
        return dto;
    }
}
