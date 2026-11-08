package com.marealata.model;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    public Long id;

    @SerializedName("channel")
    public String channel;

    @SerializedName("to")
    public String to;

    @SerializedName("subject")
    public String subject;

    @SerializedName("body")
    public String body;

    public Message(String channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public Message() {}
}
