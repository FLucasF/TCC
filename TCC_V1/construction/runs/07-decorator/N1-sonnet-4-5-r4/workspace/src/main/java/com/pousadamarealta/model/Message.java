package com.pousadamarealta.model;

public class Message {
    private Long id;
    private MessageChannel channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    public Message() {
        this.status = MessageStatus.SENT;
    }

    public Message(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
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

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}
