package com.pousada.marealta.model;

public class MessageDto {
    public long id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public MessageDto(Message m) {
        this.id = m.getId();
        this.channel = m.getChannel();
        this.to = m.getTo();
        this.subject = m.getSubject();
        this.body = m.getBody();
    }
}
