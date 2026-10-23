package com.pousada.service.messaging;

import com.pousada.domain.MessageChannel;

public class OutgoingMessage {
    private final MessageChannel channel;
    private final String originalTo;
    private String to;
    private String subject;
    private String body;
    private boolean blocked;

    public OutgoingMessage(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.originalTo = to;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.blocked = false;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public String getOriginalTo() {
        return originalTo;
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

    public boolean isBlocked() {
        return blocked;
    }

    public void block() {
        this.blocked = true;
    }
}
