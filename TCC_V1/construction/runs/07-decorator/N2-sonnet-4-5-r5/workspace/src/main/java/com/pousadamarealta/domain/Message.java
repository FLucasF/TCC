package com.pousadamarealta.domain;

public class Message {
    private Long id;
    private MessageChannel channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    public Message(Long id, MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
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
