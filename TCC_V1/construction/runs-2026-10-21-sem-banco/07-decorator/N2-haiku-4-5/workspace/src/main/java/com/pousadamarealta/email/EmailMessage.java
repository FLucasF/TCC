package com.pousadamarealta.email;

public class EmailMessage {
    private String to;
    private String subject;
    private String body;
    private boolean blocked;
    private final String originalTo;

    public EmailMessage(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.originalTo = to;
        this.blocked = false;
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

    public void block() {
        this.blocked = true;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getOriginalTo() {
        return originalTo;
    }
}
