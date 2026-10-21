package com.pousadamareaita.http.dto;

public class MessageDto {
    public int id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public MessageDto(com.pousadamareaita.model.Message msg) {
        this.id = msg.getId();
        this.channel = msg.getChannel();
        this.to = msg.getTo();
        this.subject = msg.getSubject();
        this.body = msg.getBody();
    }
}
