package com.marealta.pousada.domain.model;

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

    public Message(MessageChannel channel, String to, String subject, String body) {
        this(null, channel, to, subject, body);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
