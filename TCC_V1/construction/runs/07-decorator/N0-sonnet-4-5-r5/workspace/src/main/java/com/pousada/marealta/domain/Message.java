package com.pousada.marealta.domain;

public class Message {
    private Long id;
    private String channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    public Message() {
        this.channel = "EMAIL";
        this.status = MessageStatus.SENT;
    }

    public Message(String to, String subject, String body) {
        this();
        this.to = to;
        this.subject = subject;
        this.body = body;
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

    public void setChannel(String channel) {
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
