package com.pousada.marealta.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;
    public MessageStatus status;

    public Message(long id, String channel, String to, String subject, String body, MessageStatus status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public Message() {}
}
