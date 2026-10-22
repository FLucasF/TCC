package com.pousadamarealta.domain;

public class Message {
    private Long id;
    private final String channel;
    private final String to;
    private final String subject;
    private final String body;

    public Message(Long id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public static Message email(String to, String subject, String body) {
        return new Message(null, "EMAIL", to, subject, body);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannel() {
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
