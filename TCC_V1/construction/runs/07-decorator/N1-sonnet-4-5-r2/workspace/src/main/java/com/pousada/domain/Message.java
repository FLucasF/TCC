package com.pousada.domain;

public class Message {
    private Long id;
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;
    private final MessageStatus status;

    public Message(MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public Message withTransformedContent(String newTo, String newSubject, String newBody) {
        return new Message(this.channel, newTo, newSubject, newBody, this.status);
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

    public MessageStatus getStatus() {
        return status;
    }
}
