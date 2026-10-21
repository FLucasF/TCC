package com.pousada.dto;

import com.google.gson.annotations.SerializedName;
import com.pousada.domain.Message;

public class MessageResponse {
    @SerializedName("id")
    public int id;

    @SerializedName("channel")
    public String channel;

    @SerializedName("to")
    public String to;

    @SerializedName("subject")
    public String subject;

    @SerializedName("body")
    public String body;

    public static MessageResponse fromMessage(Message message) {
        MessageResponse response = new MessageResponse();
        response.id = message.getId();
        response.channel = message.getChannel().name();
        response.to = message.getTo();
        response.subject = message.getSubject();
        response.body = message.getBody();
        return response;
    }
}
