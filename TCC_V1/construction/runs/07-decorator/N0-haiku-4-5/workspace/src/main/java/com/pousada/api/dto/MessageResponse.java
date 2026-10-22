package com.pousada.api.dto;

public class MessageResponse {
    private Long id;
    private String channel;
    private String to;
    private String subject;
    private String body;
    private String status;

    public MessageResponse(Long id, String channel, String to, String subject, String body, String status) {
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

    public String getStatus() {
        return status;
    }
}
