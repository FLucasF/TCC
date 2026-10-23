package com.pousadamareal.api;

import com.pousadamareal.model.Message;

public class MessageResponse {
    private final long id;
    private final String channel;
    private final String to;
    private final String subject;
    private final String body;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.channel = message.getChannel();
        this.to = message.getTo();
        this.subject = message.getSubject();
        this.body = message.getBody();
    }

    public long getId() { return id; }
    public String getChannel() { return channel; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
