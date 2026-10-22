package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.MessageStatus;

public class MessageData {
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;
    private boolean shouldGenerateCopy;

    public MessageData(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
        this.shouldGenerateCopy = true;
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

    public boolean shouldGenerateCopy() {
        return shouldGenerateCopy;
    }

    public void setShouldGenerateCopy(boolean shouldGenerateCopy) {
        this.shouldGenerateCopy = shouldGenerateCopy;
    }

    public boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }
}
