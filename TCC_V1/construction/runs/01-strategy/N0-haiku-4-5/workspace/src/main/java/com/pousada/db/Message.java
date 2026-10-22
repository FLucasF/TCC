package com.pousada.db;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    @JsonProperty("id")
    public int id;

    @JsonProperty("channel")
    public String channel;

    @JsonProperty("to")
    public String to;

    @JsonProperty("subject")
    public String subject;

    @JsonProperty("body")
    public String body;

    public Message(int id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
}
