package com.pousadamarealta.domain.model;

import com.pousadamarealta.domain.enums.MessageChannel;

public class Message {
    private Long id;
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;

    public Message(Long id, MessageChannel channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public static Message email(String to, String subject, String body) {
        return new Message(null, MessageChannel.EMAIL, to, subject, body);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
