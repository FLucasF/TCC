package com.pousadamareaita.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    @JsonProperty("id")
    private long id;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("to")
    private String to;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("body")
    private String body;

    public Message(long id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public long getId() { return id; }
    public String getChannel() { return channel; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
