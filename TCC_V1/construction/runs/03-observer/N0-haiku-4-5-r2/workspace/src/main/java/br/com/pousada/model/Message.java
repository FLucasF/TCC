package br.com.pousada.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
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

    public Message() {}

    public Message(long id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
}
